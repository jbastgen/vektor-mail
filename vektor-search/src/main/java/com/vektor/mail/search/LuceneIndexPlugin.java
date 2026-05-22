package com.vektor.mail.search;

import com.vektor.mail.core.model.Message;
import com.vektor.mail.core.plugin.IndexPlugin;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;
import org.pf4j.Extension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lucene 9 full-text index for message search.
 * Index is stored per-account under ${vektor.search.index-dir}/{accountId}/
 */
@Extension
@Component
@Slf4j
public class LuceneIndexPlugin implements IndexPlugin {

    @Value("${vektor.search.index-dir:./data/index}")
    private String indexDir;

    private StandardAnalyzer analyzer;

    @PostConstruct
    public void init() {
        analyzer = new StandardAnalyzer();
    }

    @PreDestroy
    public void cleanup() {
        if (analyzer != null) analyzer.close();
    }

    @Override
    public String getPluginId() { return "vektor-search-lucene"; }

    @Override
    public void index(Message message, String plainTextBody) {
        UUID accountId = message.getMailbox().getAccount().getId();
        try (Directory dir = openDirectory(accountId);
             IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(analyzer))) {

            // Delete any existing doc for this message
            writer.deleteDocuments(new Term("id", message.getId().toString()));

            Document doc = new Document();
            doc.add(new StringField("id", message.getId().toString(), Field.Store.YES));
            doc.add(new StringField("accountId", accountId.toString(), Field.Store.YES));
            doc.add(new TextField("subject", nvl(message.getSubject()), Field.Store.YES));
            doc.add(new TextField("from", nvl(message.getFromAddress()), Field.Store.YES));
            doc.add(new TextField("body", nvl(plainTextBody), Field.Store.NO));
            writer.addDocument(doc);
        } catch (IOException e) {
            log.error("Lucene index write failed for message {}", message.getId(), e);
        }
    }

    @Override
    public List<UUID> search(String query, UUID accountId, int limit) {
        List<UUID> results = new ArrayList<>();
        try (Directory dir = openDirectory(accountId);
             DirectoryReader reader = DirectoryReader.open(dir)) {

            IndexSearcher searcher = new IndexSearcher(reader);
            MultiFieldQueryParser parser = new MultiFieldQueryParser(
                    new String[]{"subject", "from", "body"}, analyzer);
            Query q = parser.parse(query);
            TopDocs hits = searcher.search(q, limit);

            for (ScoreDoc sd : hits.scoreDocs) {
                Document doc = searcher.storedFields().document(sd.doc);
                results.add(UUID.fromString(doc.get("id")));
            }
        } catch (Exception e) {
            log.warn("Lucene search failed for query '{}': {}", query, e.getMessage());
        }
        return results;
    }

    @Override
    public void delete(UUID messageId) {
        // We don't know the account here; rely on accountId being indexed with the doc
        // In practice, search across all indexes or pass accountId
        log.warn("LuceneIndexPlugin.delete() requires accountId — use index() with empty body to overwrite");
    }

    @Override
    public void rebuildIndex(UUID accountId) {
        try (Directory dir = openDirectory(accountId);
             IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(analyzer))) {
            writer.deleteAll();
            log.info("Lucene index cleared for account {}", accountId);
        } catch (IOException e) {
            log.error("Failed to rebuild index for account {}", accountId, e);
        }
    }

    private Directory openDirectory(UUID accountId) throws IOException {
        Path path = Path.of(indexDir, accountId.toString());
        return FSDirectory.open(path);
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
