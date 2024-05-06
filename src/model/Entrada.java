package model;
 
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
 
 
@Entity
@Table(name= "Entrada")
public class Entrada {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", unique = true, nullable = true)
	private int id;
 
	@Override
	public String toString() {
		return "Entrada [id=" + id + ", instruccion=" + instruccion + "]";
	}

	@Column(name = "instruccion")
	private String instruccion;
	
	public String getInstruccion() {
		return this.instruccion;
	}
 
	public void setInstructions(String string) {
		// TODO Auto-generated method stub
		instruccion = string;
	}
}