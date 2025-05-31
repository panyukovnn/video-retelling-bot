package ru.panyukovnn.videoretellingbot.serivce.tgchatscollector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import ru.panyukovnn.videoretellingbot.client.feign.TgChatsCollectorFeignClient;
import ru.panyukovnn.videoretellingbot.dto.TgConveyorRequest;
import ru.panyukovnn.videoretellingbot.dto.chathistory.ChatHistoryResponse;
import ru.panyukovnn.videoretellingbot.model.ConveyorTag;
import ru.panyukovnn.videoretellingbot.model.ConveyorType;
import ru.panyukovnn.videoretellingbot.model.Prompt;
import ru.panyukovnn.videoretellingbot.model.PublishingChannel;
import ru.panyukovnn.videoretellingbot.model.content.Content;
import ru.panyukovnn.videoretellingbot.model.content.ContentType;
import ru.panyukovnn.videoretellingbot.model.content.Source;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEvent;
import ru.panyukovnn.videoretellingbot.model.event.ProcessingEventType;
import ru.panyukovnn.videoretellingbot.serivce.domain.ContentDomainService;
import ru.panyukovnn.videoretellingbot.serivce.domain.ProcessingEventDomainService;
import ru.panyukovnn.videoretellingbot.serivce.domain.PromptDomainService;
import ru.panyukovnn.videoretellingbot.serivce.domain.PublishingChannelDomainService;
import ru.panyukovnn.videoretellingbot.util.JsonUtil;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TgChatsCollectorHandler {

    private final JsonUtil jsonUtil;
    private final TransactionTemplate transactionTemplate;
    private final PromptDomainService promptDomainService;
    private final ContentDomainService contentDomainService;
    private final TgChatsCollectorFeignClient tgChatsCollectorFeignClient;
    private final ProcessingEventDomainService processingEventDomainService;
    private final PublishingChannelDomainService publishingChannelDomainService;

    public void handleChatMessages(TgConveyorRequest tgConveyorRequest) {
        ChatHistoryResponse chatHistory = tgChatsCollectorFeignClient.getChatHistory(
            tgConveyorRequest.getPublicChatName(),
            tgConveyorRequest.getPrivateChatNamePart(),
            tgConveyorRequest.getTopicName(),
            tgConveyorRequest.getLimit(),
            tgConveyorRequest.getDateFrom(),
            tgConveyorRequest.getDateTo());

        if (CollectionUtils.isEmpty(chatHistory.getMessageBatches())) {
            log.warn("Не найдено ни одного сообщения в указанном чате/топике");

            return;
        }

        transactionTemplate.execute(tx -> {
            PublishingChannel publishingChannel = definePublishingChannel(tgConveyorRequest);

            Prompt prompt = promptDomainService.save(Prompt.builder()
                .mapPrompt(tgConveyorRequest.getMapPrompt())
                .reducePrompt(tgConveyorRequest.getReducePrompt())
                .build());

            createContents(chatHistory, prompt.getId(), publishingChannel.getId());

            return null;
        });
    }

    private void createContents(ChatHistoryResponse chatHistory, UUID promptId, UUID publishingChannelId) {
        UUID parentBatchId = UUID.randomUUID();
        UUID childBatchId = UUID.randomUUID();

        chatHistory.getMessageBatches().forEach(messagesBatch -> {
//            LocalDate firstMessageDate = messagesBatch.getMessages().get(0).
//            LocalDate lastMessageDate = chatHistory.getLastMessageDateTime().toLocalDate();

            Content content = Content.builder()
                .link(chatHistory.getChatId().toString())
                .type(ContentType.TG_MESSAGE_BATCH)
                .source(Source.TG)
//                .title(chatHistory.getChatTitle() + "/" + chatHistory.getTopicName() + " " + firstMessageDate + " - " + lastMessageDate)
                .title(chatHistory.getChatTitle() + "/" + chatHistory.getTopicName())
                .meta(null)
                .publicationDate(chatHistory.getFirstMessageDateTime())
                .content(jsonUtil.toJson(messagesBatch.getMessages()))
                .parentBatchId(parentBatchId)
                .childBatchId(childBatchId)
                .build();

            contentDomainService.save(content);
        });

        ProcessingEvent reduceProcessingEvent = ProcessingEvent.builder()
            .type(ProcessingEventType.MAP)
            .conveyorTag(ConveyorTag.TG_MESSAGE_BATCH)
            .conveyorType(ConveyorType.MAP_REDUCE)
            .contentId(null)
            .contentBatchId(parentBatchId)
            .promptId(promptId)
            .publishingChannelId(publishingChannelId)
            .build();
        processingEventDomainService.save(reduceProcessingEvent);
    }

    private PublishingChannel definePublishingChannel(TgConveyorRequest tgConveyorRequest) {
        return publishingChannelDomainService.findByExternalId(tgConveyorRequest.getPublishingChannelExternalId())
            .orElseGet(() -> publishingChannelDomainService.save(PublishingChannel.builder()
                .externalId(tgConveyorRequest.getPublishingChannelExternalId())
                .chatId(tgConveyorRequest.getChatId())
                .topicId(tgConveyorRequest.getTopicId())
                .build()));
    }
}
