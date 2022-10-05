package care.smith.top.backend.service.nlp;

import care.smith.top.backend.repository.nlp.PhraseRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.Phrase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class PhraseService implements ContentService {

    @Autowired PhraseRepository phraseRepository;

    @Override
    @Cacheable("phraseCount")
    public long count() { return phraseRepository.count(); }

    public List<Phrase> getPhrases() {
        return List.of();
    }
}
