package com.sangeethlabs.jekyll.search;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

@WebServlet("/jsonp")
public class SearchServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static ObjectMapper objectMapper = new ObjectMapper();
    
    private static JsonFactory jsonFactory = new JsonFactory();

    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); 

    public SearchServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String queryString = req.getParameter("q");
        String domain = req.getParameter("d");
        
        String indexDirPath = System.getProperty("index.dir");
        if (indexDirPath==null) {
            System.err.println("System property 'index.dir' not specified");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        
        File indexDir = new File(indexDirPath);
        if (!indexDir.exists() || !indexDir.isDirectory()) {
            System.err.println("System property 'index.dir' not specified");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        
        if (domain==null) {
            System.err.println("Domain not specified");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        File domainIndexDir = new File(indexDirPath, domain);
        if (!domainIndexDir.exists() || !domainIndexDir.isDirectory()) {
            System.err.println("Index directory for the given domain not found");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        
        try {
            List<Post> posts = search(domainIndexDir,
                                      String.format("contents:\"%s\"",queryString));
            SearchResult result = new SearchResult();
            result.setPosts(posts);
            result.setQueryString(queryString);
            
            resp.setContentType("application/json");
            
            String json = toJson(result);
            System.out.println(json);
            
            PrintWriter out = resp.getWriter();
            out.print(String.format("jsonCallback(%s)", json));
            out.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private List<Post> search(File indexDir, String queryString) throws Exception {
        if (queryString == null || queryString.trim().length() == 0) {
            return Collections.emptyList();
        }

        String field = "contents";
        int hitsPerPage = 10;

        IndexReader reader = DirectoryReader.open(FSDirectory.open(indexDir));
        IndexSearcher searcher = new IndexSearcher(reader);
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_44);

        QueryParser parser = new QueryParser(Version.LUCENE_44, field, analyzer);
        String line = queryString;

        Query query = parser.parse(line);
        System.out.println("Searching for: " + query.toString(field));

        return doPagingSearch(searcher, query, hitsPerPage);

    }

    private List<Post> doPagingSearch(IndexSearcher searcher, Query query, int hitsPerPage) throws IOException {

        
        // Collect enough docs to show 5 pages
        TopDocs results = searcher.search(query, 5 * hitsPerPage);
        ScoreDoc[] hits = results.scoreDocs;

        int numTotalHits = results.totalHits;
        System.out.println(numTotalHits + " total matching documents");

        int start = 0;
        int end = Math.min(numTotalHits, hitsPerPage);

        if (end > hits.length) {
            hits = searcher.search(query, numTotalHits).scoreDocs;
        }

        end = Math.min(hits.length, start + hitsPerPage);

        List<Post> posts = new ArrayList<Post>(); 
        for (int i = start; i < end; i++) {

            Document doc = searcher.doc(hits[i].doc);
            String path = doc.get("path");
            if (path != null) {
                Post post = new Post();
                
                posts.add(post);
                
                post.setUrl(path);
                
                String postDate = doc.get("postDate");
                if (postDate!=null) {
                    try {
                        post.setDate(dateFormat.parse(postDate));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }                
                
                System.out.println((i + 1) + ". " + path);
                String title = doc.get("title");
                if (title != null) {
                    System.out.println("   Title: " + doc.get("title"));
                }
                post.setTitle(title);
                String excerpt = doc.get("excerpt");
                post.setExcerpt(excerpt);
                if (excerpt != null) {
                    System.out.println("   Excerpt: " + doc.get("excerpt"));
                }

            } else {
                System.out.println((i + 1) + ". "
                        + "No path for this document");
            }

        }
        
        return posts;
    }
    
    public static String toJson(Object pojo)
            throws JsonMappingException, JsonGenerationException, IOException {
        return toJson(pojo, false);
    }

    public static String toJson(Object pojo, boolean prettyPrint)
            throws JsonMappingException, JsonGenerationException, IOException {
        StringWriter buffer = new StringWriter();
        JsonGenerator generator = jsonFactory.createJsonGenerator(buffer);
        if (prettyPrint) {
            generator.useDefaultPrettyPrinter();
        }
        objectMapper.writeValue(generator, pojo);
        return buffer.toString();
    }

}

