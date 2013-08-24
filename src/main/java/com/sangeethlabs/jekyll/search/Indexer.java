package com.sangeethlabs.jekyll.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class Indexer {

    public static void main(String[] args) {
        indexFiles("/Users/sangeeth/git/openshift/corvidin/target/lucene/sangeethlabs.com",
                   "/Users/sangeeth/git/sangeeth.github.com/", 
                   true);
        indexFiles("/Users/sangeeth/git/openshift/corvidin/target/lucene/corvid.in",
                "/Users/sangeeth/git/corvid.github.com/", 
                true);
    }

    /** Index all text files under a directory. */
    public static void indexFiles(String indexPath, String docsPath, boolean create) {

        final File docDir = new File(docsPath);
        if (!docDir.exists() || !docDir.canRead()) {
            System.out
                    .println("Document directory '"
                            + docDir.getAbsolutePath()
                            + "' does not exist or is not readable, please check the path");
            System.exit(1);
        }

        Date start = new Date();
        try {
            System.out.println("Indexing to directory '" + indexPath + "'...");

            Directory dir = FSDirectory.open(new File(indexPath));
            Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_44);
            IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_44,
                    analyzer);

            if (create) {
                // Create a new index in the directory, removing any
                // previously indexed documents:
                iwc.setOpenMode(OpenMode.CREATE);
            } else {
                // Add new documents to an existing index:
                iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
            }

            // Optional: for better indexing performance, if you
            // are indexing many documents, increase the RAM
            // buffer. But if you do this, increase the max heap
            // size to the JVM (eg add -Xmx512m or -Xmx1g):
            //
            // iwc.setRAMBufferSizeMB(256.0);

            IndexWriter writer = new IndexWriter(dir, iwc);
            indexDocs(writer, docsPath, docDir);

            // NOTE: if you want to maximize search performance,
            // you can optionally call forceMerge here. This can be
            // a terribly costly operation, so generally it's only
            // worth it when your index is relatively static (ie
            // you're done adding documents to it):
            //
            // writer.forceMerge(1);

            writer.close();

            Date end = new Date();
            System.out.println(end.getTime() - start.getTime()
                    + " total milliseconds");

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass()
                    + "\n with message: " + e.getMessage());
        }
    }

    static void indexDocs(IndexWriter writer, String docsPath, File file)
            throws IOException {
        // do not try to index files that cannot be read
        if (file.canRead()) {
            if (file.isDirectory()) {
                String[] files = file.list(new FilenameFilter() {

                    @Override
                    public boolean accept(File dir, String name) {
                        return new File(dir, name).isDirectory()
                                || name.endsWith(".textile");
                    }
                });
                // an IO error could occur
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        indexDocs(writer, docsPath, new File(file, files[i]));
                    }
                }
            } else {

                FileInputStream fis;
                try {
                    fis = new FileInputStream(file);
                } catch (FileNotFoundException fnfe) {
                    // at least on windows, some temporary files raise this
                    // exception with an "access denied" message
                    // checking if the file can be read doesn't help
                    return;
                }

                try {

                    // make a new, empty document
                    Document doc = new Document();

                    // Add the path of the file as a field named "path". Use a
                    // field that is indexed (i.e. searchable), but don't
                    // tokenize
                    // the field into separate words and don't index term
                    // frequency
                    // or positional information:
                    String path = file.getParent();
                    String fileName = file.getName();
                    path = path.substring(docsPath.length() - 1);
                    int _postIndex = path.indexOf("/_posts");
                    if (_postIndex > -1) {
                        String date = fileName.substring(0, 10);
                        Field postDateField = new StringField("postDate", date,
                                Field.Store.YES);
                        doc.add(postDateField);
                        fileName = fileName.substring(11);
                        path = path.replaceAll("/_posts", "");
                    }
                    fileName = fileName.replaceAll(".textile", ".html");
                    path = path + "/" + fileName;

                    System.out.println("Path: " + path);
                    Field pathField = new StringField("path", path,
                            Field.Store.YES);
                    doc.add(pathField);

                    doc.add(new LongField("modified", file.lastModified(),
                            Field.Store.NO));

                    doc.add(new TextField("contents", new BufferedReader(
                            new InputStreamReader(fis, "UTF-8"))));
                    Map<String, String> pageHeaders = fetchPageHeaders(file);
                    for (Map.Entry<String, String> e : pageHeaders.entrySet()) {
                        Field field = new StringField(e.getKey(), e.getValue(),
                                Field.Store.YES);
                        doc.add(field);
                    }

                    if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
                        // New index, so we just add the document (no old
                        // document can be there):
                        System.out.println("adding " + file);
                        writer.addDocument(doc);
                    } else {
                        // Existing index (an old copy of this document may have
                        // been indexed) so
                        // we use updateDocument instead to replace the old one
                        // matching the exact
                        // path, if present:
                        System.out.println("updating " + file);
                        writer.updateDocument(new Term("path", file.getPath()),
                                doc);
                    }

                } finally {
                    fis.close();
                }
            }
        }
    }

    private static Map<String, String> fetchPageHeaders(File textileFile)
            throws IOException {
        Map<String, String> pageHeaders = new TreeMap<String, String>();

        BufferedReader in = new BufferedReader(new FileReader(textileFile));
        // ---
        // layout: post
        // title: Hello World ! Using Java Native Interface
        // tags: Java, JNI, Native, C++, C
        // excerpt: A simple step by step guide to write your first Hello world
        // program using Java Native Interface
        // ---
        String line = null;

        boolean headers = false;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("---")) {
                if (headers) {
                    break;
                }
                headers = true;
            } else if (headers) {
                int i = line.indexOf(":");
                if (i > 0 && i < line.length() - 1) {
                    String key = line.substring(0, i);
                    String value = line.substring(i + 1);
                    pageHeaders.put(key, value);
                }
            }
        }

        in.close();

        return pageHeaders;
    }
}
