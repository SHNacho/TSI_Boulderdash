package src_Sanchez_Herrera_Ignacio;

import tools.Vector2d;
import ontology.Types.ACTIONS;
import core.game.StateObservation;
import core.game.Observation;
import java.util.Objects;



import java.util.ArrayList;
import java.util.Stack;

//
enum Orientation{
    NORTE,
    SUR,
    ESTE,
    OESTE;
}

/**
 * Clase que representa un nodo del grafo.
 * Dado que vamos a realizar un algoritmo A*, cada nodo contiene:
 * - La función g()
 * - La función h()
 * - La posición del nodo en el grid del mundo
 * - La orientación del nodo (Norte, Sur, ...)
 * - La acción que ha llevado al nodo (necesaria para obtener la lista de acciones
 *   una vez que hemos lleagado al nodo objetivo)
 * - El nodo padre
 *
 * Se han utilizado variables de clase que serán comunes a todas las instancias de
 * la clase, que son:
 * - El destino (posición de destino) necesaria para calcular la función h()
 * - Un objeto del tipo StateObservation, necesario para concer los objetos que hay
 *   en el mapa y su posición (útil a la hora de crear los hijos de un nodo, para
 *   no crear un hijo con una posición en la que hay un muro, por ejemplo).
 */
public class Node implements Comparable<Node>{
	// Atributos de clase
    static Vector2d dest;               // Posición de destino
	static StateObservation stateObs;

	// Atributos de instancia
    private int g;										// Coste del camino hasta ese nodo
    private int h;                      				// Coste de la función heurística en ese nodo 
														// (distancia manhattan)
    private Vector2d position = new Vector2d();         // Posición del nodo
    private Orientation orientation;					// Orientación del nodo
    private ACTIONS action;             				// Acción que ha llevado a ese nodo
    private Node parent;								// Nodo padre
	private ArrayList<Node> childs = new ArrayList<>(); // Hijos del nodo


    /**
     * Constructor que se usará para el nodo raíz
     * @param position vector2d que contiene la posicion inicial del avatar
     * @param orientation vector2d que contiene la orientacion inicial del avatar
     */
    public Node(Vector2d position, Vector2d orientation, Vector2d dest, StateObservation stateObs){
        this.g = 0;
        this.parent = null;
        this.position = position;
        this.action = null;
        Node.stateObs = stateObs;
        this.orientation = vectorToOrientation(orientation);
        Node.dest = dest;
        calculateHeuristic();

    }

    /**
     * Constructor que se empleará para crear los nodos hijos de un nodo
     * @param parent Nodo padre
     * @param action Acción que ha llevado a este nodo
     */
    public Node(Node parent, ACTIONS action){
        this.action = action;
        this.parent = parent;
        calculatePosition();
		calculateG();
        calculateHeuristic();
    }

    /**
     * Transforma un vector de orientación a un enumerado del tipo Orientation
     * @param v_orientation vector de orientación
     * @return Orientación del nodo
     */
    private Orientation vectorToOrientation(Vector2d v_orientation){
    
        Orientation orientacion = null;

        if(v_orientation.x == 1.0 && v_orientation.y == 0.0){
            orientacion = Orientation.ESTE;
        }
        if(v_orientation.x == -1.0 && v_orientation.y == 0.0){
            orientacion = Orientation.OESTE;
        }
        if(v_orientation.x == 0.0 && v_orientation.y == 1.0){
            orientacion = Orientation.SUR;
        }
        if(v_orientation.x == 0.0 && v_orientation.y == -1.0){
            orientacion = Orientation.NORTE;
        }
        return orientacion;
    }

    /**
     * Calculamos el coste de la función g()
     * Por cada casilla que se avance:
     * - Si no ha sido necesario un cambio de dirección -> g(hijo) = g(padre) + 1
     * - Si ha sido necesario cambio de dirección -> g(hijo) = g(padre) + 2
	 * Si el nodo tiene hijos, recalculamos también su función g()
     */
    private void calculateG(){
        g = parent.g + 1;
        if(this.orientation != parent.orientation){
            g += 1;
        }

		if(!childs.isEmpty()){
			for(Node h:childs){
				h.calculateG();
			}
		}
    }

    /**
     * Calculamos la orientación del nodo hijo y su posicion
     * en función de la acción realizada
     * Si nos hemos movido hacia abajo -> orientación = SUR
     * .
     * .
     * .
     */
    private void calculatePosition(){
        switch (action)
        {
            case ACTION_DOWN:
                orientation = Orientation.SUR;
                position.y = parent.position.y + 1;
				position.x = parent.position.x;
                break;
            case ACTION_LEFT:
                orientation = Orientation.OESTE;
                position.x = parent.position.x - 1;
                position.y = parent.position.y;
                break;
            case ACTION_RIGHT:
                orientation = Orientation.ESTE;
                position.x = parent.position.x + 1;
                position.y = parent.position.y;
                break;
            case ACTION_UP:
                orientation = Orientation.NORTE;
                position.y = parent.position.y - 1;
				position.x = parent.position.x;
                break;
        }
    }

    /**
     * Calcula la función heurística h() y la almacena en
     * la variable de instancia 'h'
     */
    public void calculateHeuristic(){
//        if(position.x != dest.x && position.y != dest.y){
//
//        }
        h = (int) (Math.abs(position.x - dest.x) + Math.abs(position.y - dest.y));
        h += Agent.nivelPeligro(position, stateObs);

    }
	
	/**
	 * Establece el padre del nodo y vuelve a calcular la función g()
	 * que depende del padre
	 * */
	public void setParent(Node parent){
		this.parent = parent;
		calculateG();
	}

    /**
     * Devuelve la posicion del nodo
     * @return posicion del nodo
     */
    public Vector2d getPosition(){
        return position;
    }
	/**
	* Obtiene el valor de la función en ese nodo
	* @return Valor de la función f()=g()+h()
	 */
    public int getFunction(){
        return g+h;
    }

	/**
	* Obtiene el padre del nodo
	* @return Padre del nodo
	 */
	public Node getParent(){
		return parent;
	}

	public Vector2d getDest(){ return dest; }

	/** 
	 * Genera los hijos del nodo.
	 * No se generarán hijos con una posición en la que haya un enemigo
	 * o un muro.
	 * Se tendrá en cuenta la acción que ha llevado a este nodo, para no
	 * generar un nodo con la misma posición que el nodo padre, ya que no
	 * tendría sentido
	 */
    public ArrayList<Node> generateChilds(){

		if(this.action != null){ //Si no es el nodo raíz
			switch(this.action){
			case ACTION_UP:
                childs.add(generateChild(ACTIONS.ACTION_UP));
				childs.add(generateChild(ACTIONS.ACTION_LEFT));
				childs.add(generateChild(ACTIONS.ACTION_RIGHT));
				break;
			case ACTION_DOWN:
				childs.add(generateChild(ACTIONS.ACTION_DOWN));
				childs.add(generateChild(ACTIONS.ACTION_LEFT));
				childs.add(generateChild(ACTIONS.ACTION_RIGHT));
				break;
			case ACTION_LEFT:
                childs.add(generateChild(ACTIONS.ACTION_UP));
                childs.add(generateChild(ACTIONS.ACTION_DOWN));
				childs.add(generateChild(ACTIONS.ACTION_LEFT));
				break;
			case ACTION_RIGHT:
                childs.add(generateChild(ACTIONS.ACTION_UP));
                childs.add(generateChild(ACTIONS.ACTION_DOWN));
				childs.add(generateChild(ACTIONS.ACTION_RIGHT));
				break;
			}
		}
		else{ // Si es el nodo raíz genera los cuatro hijos
            childs.add(generateChild(ACTIONS.ACTION_UP));
            childs.add(generateChild(ACTIONS.ACTION_DOWN));
			childs.add(generateChild(ACTIONS.ACTION_LEFT));
			childs.add(generateChild(ACTIONS.ACTION_RIGHT));
		}

		// Eliminamos los elementos nulos del vector de hijos.
		childs.removeIf(Objects::isNull);

        return childs;
    }


	/**
	 * Comprueba si una acción es válida, es decir, que es posible moverse
	 * a la posición que lleva esa acción.
	 * */
	private boolean validAction(ACTIONS action){

		Vector2d pos = new Vector2d(position);
		int elementId;

		switch (action)
        {
            case ACTION_DOWN:
                pos.y = this.position.y + 1;
                break;
            case ACTION_LEFT:
                pos.x = this.position.x - 1;
                break;
            case ACTION_RIGHT:
                pos.x = this.position.x + 1;
                break;
            case ACTION_UP:
                pos.y = this.position.y - 1;
                break;
        }

		elementId = getElement(pos);

		// Si en esa posición no hay enemigos ni muros, es válida
		if(elementId != 0 && elementId != 10 && elementId != 11)
			return true;
		
		else
			return false;
	}

	/** 
	 * Obtiene el id del elemento que está en la posición 'position'
	 * @param position posicion del grid
	 * @return id del elemento en esa posición. -1 si no hay nada.
	 * */
	//TODO Eliminar este método y utilizar el de la clase Agent
	private int getElement(Vector2d position){

		ArrayList<Observation> aux = stateObs.getObservationGrid()[(int)position.x][(int)position.y];

		if(!aux.isEmpty()){
			return aux.get(0).itype;
		}

		return -1;
	}

	/**
	 * Genera un hijo si la acción es válida
	 * @param action Acción que genera el hijo
	 * @return Nodo hijo. En caso de que la acción no sea válida devuelve null
	 * */
	public Node generateChild(ACTIONS action){
		if(validAction(action))
			return new Node(this, action);
		else
			return null;
	}

	/**
	 * Obtiene la lista de acciones que ha llevado hasta el nodo actual
	 * @param list lista de acciones en la que almacenar las acciones
	 * @return Esa misma lista de acciones
	 * */
	public Stack<ACTIONS> actionsList(Stack<ACTIONS> list){
	    if(this.action != null){
            list.push(this.action);
            if(this.orientation != parent.orientation )
                list.push(this.action);

            parent.actionsList(list);
        }

		return list;	
	}

	

    /**
     * Compara dos elementos del tipo nodo. La variable de comparación
     * es su función f()=g()+h()
     */
    @Override
    public int compareTo(Node n){

        //En el caso de que la distancia sea la misma, es necesario
        //comparar que tienen distinta posicion, ya que al almacenarlos
        //en un SortedSet no permite que haya dos elementos iguales, por lo
        //que se diferenciaran por la posicion.
        if(this.position.x == n.position.x && this.position.y == n.position.y)
            return 0;
        else if(this.getFunction() > n.getFunction())
            return 1;
        else
            return -1;
    }

    @Override
    public String toString() {
        return "Posicion: " + position.toString() + "\n g: " + g + "\n h: " + h + "\n Orientacion: " + orientation;
    }
}
