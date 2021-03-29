package io.freedriver.autonomy.service;

import io.freedriver.autonomy.cdi.qualifier.SpeechCache;
import io.freedriver.autonomy.jpa.entity.event.speech.SpeechEvent;
import io.freedriver.autonomy.jpa.entity.event.speech.SpeechEventType;
import io.freedriver.autonomy.service.crud.JPACrudService;
import io.freedriver.base.util.Festival;
import org.infinispan.Cache;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@ApplicationScoped
public class SpeechService extends JPACrudService<SpeechEvent> {

    @Inject
    @SpeechCache
    Cache<String, SpeechEvent> speechCache;

    @Override
    public Class<SpeechEvent> getEntityClass() {
        return SpeechEvent.class;
    }

    @Transactional
    public void observeEvent(@Observes SpeechEvent speechEvent) {
        speak(speechEvent);
    }

    private boolean shouldActOnEvent(SpeechEvent event) {
        return
                event.getSpeechEventType() == SpeechEventType.IMPERATIVE
                ||
                        (!haveRecentActivityOnSubject(event) && !lastActivityOnSubjectIdentical(event));
    }

    // Don't spam stuff.
    private boolean haveRecentActivityOnSubject(SpeechEvent event) {
        if (speechCache.containsKey(event.getSubject())) {
            return Instant.ofEpochMilli(speechCache.get(event.getSubject()).getTimestamp())
                    .plus(Duration.ofSeconds(20))
                    .isAfter(Instant.now());
        }
        return false;
    }

    // Ensure we don't repeat messages.
    private boolean lastActivityOnSubjectIdentical(SpeechEvent event) {
        if (speechCache.containsKey(event.getSubject())) {
            return Objects.equals(event.getText(), speechCache.get(event.getSubject()).getText());
        }
        return false;
    }

    private synchronized void speak(SpeechEvent event) {
        if (shouldActOnEvent(event)) {
            Festival.speak(event.getText());
            speechCache.put(event.getSubject(), event);
            persist(event);
        }
    }
}
