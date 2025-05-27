package ru.panyukovnn.videoretellingbot.serivce.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.panyukovnn.videoretellingbot.model.PublishingChannel;
import ru.panyukovnn.videoretellingbot.repository.PublishingChannelRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicationChannelDomainService {

    private final PublishingChannelRepository publishingChannelRepository;

    public PublishingChannel save(PublishingChannel publishingChannel) {
        return publishingChannelRepository.save(publishingChannel);
    }

}
