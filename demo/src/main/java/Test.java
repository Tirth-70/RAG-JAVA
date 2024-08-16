// import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

// import org.apache.pdfbox.pdmodel.PDDocument;
// import org.apache.pdfbox.text.PDFTextStripper;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.model.mistralai.MistralAiChatModel;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.*;

// import org.apache.pdfbox.Loader;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;


public class Test extends JFrame implements ActionListener{
    Assistant ass;
    EmbeddingStore<TextSegment> embeddingStore;


    static JTextArea area=new JTextArea();
	JTextField field=new JTextField();
	JScrollPane sp;
	JButton send;
	LocalTime time=LocalTime.now();
	LocalDate date=LocalDate.now();
	Random random=new Random();

    public Test(String title, Assistant assistant){
        super(title);
        ass = assistant;
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(null);
		setResizable(false);
		getContentPane().setBackground(Color.cyan);
		field=new JTextField();
		send=new JButton(">");
		send.setFont(new Font("Serif",Font.BOLD,25));
		send.setBackground(Color.white);
		send.setBounds(735,520,50,35);
		add(send);
		//For Text area
		area.setEditable(false);
		area.setBackground(Color.white);
		add(area);
		area.setFont(new Font("Serif",Font.PLAIN,20));
		//scrollbar
		sp=new JScrollPane(area,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setBounds(10,20,775,470);
		add(sp);
			
		//For TextField
		field.setSize(725,35);
		field.setLocation(10,520);
		field.setForeground(Color.black);
		field.setFont(new Font("Serif",Font.BOLD,25));
		add(field);
		
		send.addActionListener(this);
		getRootPane().setDefaultButton(send);
    }

    public void actionPerformed(ActionEvent e)
	{
		String question=field.getText().toLowerCase();
	   
		area.append("You : "+field.getText()+"\n");
		field.setText("");
		Socket sock=new Socket();
		if (question.equals("exit")) {
            getEmbeddingStore().removeAll();
            System.exit(0);
        }
        bot(ass.answer(question));
		
	}
	public static void bot(String question)
	{
		area.append("Bot : "+question+"\n");
	}

    public static void main(String[] args) throws Exception{

        Test cb=new Test("Chat Bot", createAssistant("documents/"));
		cb.setSize(800,605);
		cb.setLocation(50,50);
    }

    public static EmbeddingStore<TextSegment> getEmbeddingStore() {
        EmbeddingStore<TextSegment> store = PineconeEmbeddingStore.builder()
        .apiKey("<API>")
        .environment("aped-4627-b74a")
        // Project ID can be found in the Pinecone url:
        // https://app.pinecone.io/organizations/{organization}/projects/{environment}:{projectId}/indexes
        .projectId("xsda5ax")
        // Make sure the dimensions of the Pinecone index match the dimensions of the embedding model
        // (384 for all-MiniLM-L6-v2, 1536 for text-embedding-ada-002, etc.)
        .index("rag-java")
        .build();
        return store;
    }

    private static Assistant createAssistant(String documentPath) {

        ChatLanguageModel chatLanguageModel = MistralAiChatModel.builder()
                    .apiKey("<API>") // Please use your own Mistral AI API key
                    .modelName("mistral-small")
                    .logRequests(true)
                    .logResponses(true)
                    .build();


        List<Document> documents = loadMultipleDocumentsRecursively(documentPath);

        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        
        EmbeddingStore<TextSegment> embeddingStore = getEmbeddingStore();
        

        List<TextSegment> segments = splitter.splitAll(documents);
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);
        
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(5) // on each interaction we will retrieve the 2 most relevant segments
                .minScore(0.5) // we want to retrieve segments at least somewhat similar to user query
                .build();

        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        return AiServices.builder(Assistant.class)
                .chatLanguageModel(chatLanguageModel)
                .contentRetriever(contentRetriever)
                .chatMemory(chatMemory)
                .build();

    }



    private static List<Document> loadMultipleDocumentsRecursively(String documentPath) {
        Path directoryPath = toPath(documentPath);
        System.out.println("Loading multiple documents recursively from: {}"+ directoryPath);
        List<Document> documents = loadDocumentsRecursively(directoryPath, new ApacheTikaDocumentParser());
        // documents.forEach(Test::log);
        System.out.println("");
        return documents;
    }


    private static Path toPath(String fileName) {
        try {
            URL fileUrl = Test.class.getResource(fileName);
            return Paths.get(fileUrl.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
