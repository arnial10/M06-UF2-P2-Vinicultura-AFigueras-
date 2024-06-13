package manager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
 
 
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mysql.cj.x.protobuf.MysqlxDatatypes.Array;
import com.mysql.cj.xdevapi.UpdateResult;

import model.Bodega;
import model.Campo;
import model.Entrada;
import model.Vid;
import utils.TipoVid;
 
public class Manager {
	private static Manager manager;
	private ArrayList<Entrada> entradas;
	private Session session;
	private Transaction tx;
	private Bodega b;
	private Campo c;
	MongoCollection <Document> collection;
	MongoDatabase database;

	private Manager () {
		this.entradas = new ArrayList<>();
		 
       
	}
	public static Manager getInstance() {
		if (manager == null) {
			manager = new Manager();
		}
		return manager;
	}
	
	 private int getVendimia() {

	        MongoCollection<Document> vendimias = database.getCollection("vendimias");
	        System.out.println("ola");
	        collection = database.getCollection("vendimias");
	        Document Vendimia = vendimias.find().sort(Sorts.descending("counter")).first();
	        if (Vendimia != null) {
	            return Vendimia.getInteger("counter", 0);
	        } else {
	            return 0;
	        }
	    }
	
	private void createSession() {
	    //org.hibernate.SessionFactory sessionFactory = new Configuration().configure("hibernate.cfg.xml").buildSessionFactory();
//	    session = sessionFactory.openSession();
	    //conexion mongodb
	    String uri = "mongodb://localhost:27017";
	    MongoClientURI mongoClientURI = new MongoClientURI(uri);
	    MongoClient mongoClient = new MongoClient(mongoClientURI);
	    database = mongoClient.getDatabase("MongoDBArnauFigueras");
	}

	
	public void init() {
		createSession();
		getEntrada();
		manageActions();
		//showAllCampos();
		//session.close();
	}
 
	private void manageActions() {
		for (Entrada entrada : this.entradas) {
			try {
				System.out.println(entrada.getInstruccion());
				switch (entrada.getInstruccion().toUpperCase().split(" ")[0]) {
					case "B":
						addBodega(entrada.getInstruccion().split(" "));
						break;
					case "C":
						addCampo(entrada.getInstruccion().split(" "), false);
						break;
					case "V":
						addVid(entrada.getInstruccion().split(" "));
						break;
					case "M":
					    markAsVendimiado(entrada.getInstruccion().split(" "));
					    break;
					case "#":
						vendimia(entrada.getInstruccion().split(""));
						break;
					default:
						System.out.println("Instruccion incorrecta");
				}
			} catch (HibernateException e) {
				e.printStackTrace();
				if (tx != null) {
					tx.rollback();
				}
			}
		}
	}
 
	private void vendimia(String[] split) {
        this.b.getVids().addAll(this.c.getVids());
        tx = session.beginTransaction();
        session.save(b);
        tx.commit();
 
        getVendimia();
       
    }
 
   
 
	/*private void addVid(String[] split) {
		Vid v = new Vid(TipoVid.valueOf(split[1].toUpperCase()), Integer.parseInt(split[2]));
		tx = session.beginTransaction();
		session.save(v);
		c.addVid(v);
		session.save(c);
		tx.commit();
	}*/
	private void addVid(String[] split) {
		Vid v = new Vid(TipoVid.valueOf(split[1].toUpperCase()), Integer.parseInt(split[2]));
		collection = database.getCollection("campo");
		Document lastCampo = collection.find().sort(new Document("_id", -1)).first();
		collection = database.getCollection("vid");
		Document document = new Document().append("type", v.getVid().toString()).append("quantity", v.getCantidad()).append("campo", lastCampo);
		collection.insertOne(document);
		//ad on vid
		Document document2 = new Document().append("type", v.getVid().toString()).append("quantity", v.getCantidad());
		collection = database.getCollection("campo");
		Document update = new Document("$push", new Document("vid", document2));
		collection.updateOne(lastCampo, update);
	}

 
	/*private void addCampo(String[] split) {
		c = new Campo(b);
		tx = session.beginTransaction();
		int id = (Integer) session.save(c);
		c = session.get(Campo.class, id);
		tx.commit();
	}*/
	private void addCampo(String[] split, boolean vendimiado){
		c = new Campo(b);
		collection = database.getCollection("bodega");
		Document lastBodega = collection.find().sort(new Document("_id", -1)).first();
		collection = database.getCollection("campo");
		Document document = new Document().append("nombre", lastBodega);
		   document.put("vendimiado", vendimiado);
		   Campo nuevoCampo = new Campo();
		   nuevoCampo.setVendimiado(vendimiado); // Establecer el valor de vendimiado en el objeto Campo// Establecer el valor de vendimiado
		collection.insertOne(document);
		
	}

	/*private void addBodega(String[] split) {
		b = new Bodega(split[1]);
		tx = session.beginTransaction();
		int id = (Integer) session.save(b);
		b = session.get(Bodega.class, id);
		tx.commit();
	}*/
	public void addBodega(String[] split) {
		b = new Bodega(split[1]);
		collection = database.getCollection("bodega");
		Document document = new Document().append("nombre", b.getNombre());
		collection.insertOne(document);
	}
 
	/*private void getEntrada() {
		tx = session.beginTransaction();
		Query q = session.createQuery("select e from Entrada e");
		this.entradas.addAll(q.list());
		tx.commit();
	}*/
	public void getEntrada() {
		collection = database.getCollection("entrada");
		for(Document documents : collection.find()) {
			Entrada entry = new Entrada();
			entry.setInstructions(documents.getString("Entrada"));
			entradas.add(entry);
		}
		System.out.println(entradas.toString());
		
	}
	private void markAsVendimiado(String[] parts) {
	    if (parts.length >= 2) {
	        String campoId = parts[1];
	       
	        // Crear el filtro para buscar el campo por su ID
	        Bson filter = Filters.eq("_id", new ObjectId(campoId));
	       
	        // Crear la actualización para establecer "vendimiado" en true
	        Bson update = Updates.set("vendimiado", true);
	       
	        // Ejecutar la actualización en la colección de campos
	        MongoCollection<Document> campoCollection = database.getCollection("campo");
	        com.mongodb.client.result.UpdateResult updateResult = campoCollection.updateOne(filter, update);
	       
	        // Verificar si se realizó la actualización correctamente
	        if (updateResult.getModifiedCount() > 0) {
	            System.out.println("Campo marcado como vendimiado correctamente.");
	        } else {
	            System.out.println("No se encontró ningún campo con el ID proporcionado.");
	        }
	    } else {
	        System.out.println("La instrucción no tiene el formato esperado.");
	    }
	}
	/*private void showAllCampos() {
		tx = session.beginTransaction();
		Query q = session.createQuery("select c from Campo c");
		List<Campo> list = q.list();
		for (Campo c : list) {
			System.out.println(c);
		}
		tx.commit();
	}*/
}