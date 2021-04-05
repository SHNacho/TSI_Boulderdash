package src_Sanchez_Herrera_Ignacio;

import com.sun.source.tree.NewArrayTree;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.*;
import java.util.ArrayList;

public class Agent extends AbstractPlayer {
	
    static Vector2d fescala;
    Vector2d portal;
    Vector2d avatar;
	Stack<ACTIONS> camino = new Stack<>();
    Integer nivel;

    /**
     * initialize all variables for the agent
     * @param stateObs Observation of the current state.:
     * @param elapsedTimer Timer when the action returned is due.
     */
    public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        //Calculamos el factor de escala entre mundos (pixeles -> grid)
        fescala = new Vector2d(stateObs.getWorldDimension().width / stateObs.getObservationGrid().length ,
                stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);


        //Se crea una lista de observaciones de portales, ordenada por cercania al avatar
        ArrayList<Observation>[] posiciones = stateObs.getPortalsPositions(stateObs.getAvatarPosition());
        //Seleccionamos el portal
        portal = posiciones[0].get(0).position;
        portal.x = Math.floor(portal.x / fescala.x);
        portal.y = Math.floor(portal.y / fescala.y);
        //Calculamos la posición del avatar
        avatar = stateObs.getAvatarPosition();
        avatar.x = Math.floor(avatar.x / fescala.x);
        avatar.y = Math.floor(avatar.y / fescala.y);


        //Elegimos el comportamiento que debe tener el jugador en funcion de los enemigos y las gemas
        //Si no hay enemigos ni gemas, nos encontramos en el nivel 1
        if(stateObs.getNPCPositions() == null && stateObs.getResourcesPositions() == null) {
            nivel = 1;
            Node rootNode = new Node(avatar, stateObs.getAvatarOrientation(), portal, stateObs);
            deliberativoSimple(rootNode);
        }
        //Si hay gemas y no hay enemigos, estamos en el nivel 2
        else if (stateObs.getNPCPositions() == null && stateObs.getResourcesPositions() != null) {
            nivel = 2;
        }
        else if (stateObs.getNPCPositions() != null && stateObs.getResourcesPositions() == null) {
			nivel = 3;
		}
        else
        	nivel = 5;
    }

    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        switch (nivel){
            // Si el nivel es 1, ya hemos calculado el camino en el constructor
            case 1:
                return camino.pop();
            // Si el nivel es 2
            case 2:
                // Si no hay camino calculado, calculamos el camino hasta la gema más cercana
                if(camino.isEmpty()){
					// Calculamos la posición actual del avatar en coordenadas del grid
                    avatar = stateObs.getAvatarPosition();
                    avatar.x = Math.floor(avatar.x / fescala.x);
                    avatar.y = Math.floor(avatar.y / fescala.y);
					
					// Si no hemos encontrado 9 gemas todavía

                    if(stateObs.getAvatarResources().isEmpty() || stateObs.getAvatarResources().get(6) < 9){
						// Calculamos la posición de la gema más cercana al avatar
                        Vector2d siguienteGema = gemaMasCercana(stateObs);

						// Buscamos el camino con A* hasta esa gema 
                        Node rootNode = new Node(avatar, stateObs.getAvatarOrientation(), siguienteGema, stateObs);
                        camino = findPath(rootNode);
                    }
                    else{
                        Node rootNode = new Node(avatar, stateObs.getAvatarOrientation(), portal, stateObs);
                        camino = findPath(rootNode);
                    }
                }

//                avatar = stateObs.getAvatarPosition();
//                avatar.x = Math.floor(avatar.x / fescala.x);
//                avatar.y = Math.floor(avatar.y / fescala.y);

//                if(gemasEncontradas < 9){
//                    Vector2d siguienteGema = gemaMasCercana(stateObs);
//                    Node rootNode = new Node(avatar, stateObs.getAvatarOrientation(), siguienteGema, stateObs);
//                    camino = findPath(rootNode);
//                }
//                else{
//                    Node rootNode = new Node(avatar, stateObs.getAvatarOrientation(), portal, stateObs);
//                    camino = findPath(rootNode);
//                }
//
//                ArrayList<Observation> aux = stateObs.getObservationGrid()[(int)avatar.x][(int)avatar.y];
//                System.out.println(aux.get(0).itype);
//                if(!aux.isEmpty()){
//                    if (aux.get(0).itype == 6){
//                        gemasEncontradas++;
//                        System.out.println(gemasEncontradas);
//                    }
//
//                }
                return camino.pop();
            case 3:
			case 4:
				avatar = getGridPosition(stateObs.getAvatarPosition());
				if(nivelPeligro (avatar, stateObs) > 0){
                    //System.out.println("Hay enemigos");
					Vector2d casilla_segura = buscarCasillaSegura(stateObs);
                    //System.out.println(casilla_segura);
					Node rootNode = new Node(avatar, stateObs.getAvatarOrientation(), casilla_segura, stateObs);
				    camino = findPath(rootNode);	
					return camino.pop();
				}
				break;

			case 5:
				avatar = getGridPosition(stateObs.getAvatarPosition());
				Vector2d objetivo;
				if(stateObs.getAvatarResources().isEmpty() || stateObs.getAvatarResources().get(6) < 9){
					// Calculamos la posición de la gema más cercana al avatar
					objetivo = gemaMasCercana(stateObs);
				}
				else
					objetivo = portal;

				if(camino.isEmpty() || nivelPeligro(avatar, stateObs) > 0){
//					if(nivelPeligro(avatar, stateObs) > 0 && (getElement(objetivo, stateObs) == 10
//														|| getElement(objetivo, stateObs) == 11))
//					{
//						objetivo = buscarCasillaSegura(stateObs);
//					}
					if(nivelPeligro(avatar,stateObs) >= 4)
						objetivo = buscarCasillaSegura(stateObs);

					Node rootNode = new Node(avatar, stateObs.getAvatarOrientation(), objetivo, stateObs);
					camino = findPath(rootNode);
				}
				return camino.pop();
        }
        return null;
    }

	/**
	* A partir de un nodo raíz encuentra el camino más cercano al destino
	* Este destino se encuentra en el propio nodo raíz
	* @param rootNode Nodo de partida, con la posición inicial y el destino
	* @return Una pila con la lista de acciones hasta llegar al destino
	 */
    public Stack<ACTIONS> findPath(Node rootNode) {
        TreeSet<Node> abiertos = new TreeSet<>();
        TreeSet<Node> cerrados = new TreeSet<>();
        Stack<ACTIONS> camino = new Stack<>();

        abiertos.add(rootNode);
        Node nodoActual = abiertos.pollFirst();


        while((nodoActual.getPosition().x != nodoActual.getDest().x) ||
			  (nodoActual.getPosition().y != nodoActual.getDest().y))
		{
            // Generamos los hijos del nodo
			ArrayList<Node> hijos = nodoActual.generateChilds();

			for(Node n:hijos){
				// Si el nodo está en abiertos;
				if(abiertos.contains(n)){
					// Comprobamos si el camino nuevo es mejor que el que ya tenía 
					// y nos quedamos con el mejor padre
					Node aux = abiertos.ceiling(n);	
					if(aux.getFunction() > n.getFunction())
						aux.setParent(n.getParent());
				}
				//Si el nodo ya está en cerrados
				else if(cerrados.contains(n)){
					// Comprobamos si el nuevo camino es mejor que el anterior y
					// nos quedamos con el mejor padre. Además necesitamos modificar
					// el valor de g() para todos sus hijos si el padre cambia
					Node aux = cerrados.ceiling(n);
					if(aux.getFunction() > n.getFunction())
						aux.setParent(n.getParent());
				}
				// Si no está en abiertos ni en cerrados, metemos el nodo en abiertos.
				else
					abiertos.add(n);
			}


			cerrados.add(nodoActual);
			nodoActual = abiertos.pollFirst();
        }

		nodoActual.actionsList(camino);	

        return camino;
    }

    public void deliberativoSimple (Node rootNode){
        camino = findPath(rootNode);
    }

    public void deliberativoCompuesto (Node rootNode){
    }

    /**
     * Obtiene la gema más cercana al avatar
     * @param stateObs Objeto del tipo StateObservation con el que obtnemos las gemas
     * @return Posición de la gema más cercana en el Grid
     */
    public Vector2d gemaMasCercana (StateObservation stateObs){
        ArrayList<Observation> gemas = stateObs.getResourcesPositions()[0];
        ArrayList<Vector2d> gemasGrid = new ArrayList<>(); // Vector de gemas en coordenadas del grid

		//Pasamos las coordenadas de todas las gemas a coordenadas del grid
        for(Observation g:gemas){
            gemasGrid.add(new Vector2d(Math.floor(g.position.x / fescala.x), Math.floor(g.position.y / fescala.y)));
        }

		// Inicializamos la mas cercana a la primera del vector
        int menorDistancia = distanciaManhattan(avatar, gemasGrid.get(0));
        Vector2d posMasCercana = gemasGrid.get(0);

		// Buscamos la más cercana en distancia Manhattan
        for(Vector2d g:gemasGrid){
            int distancia = distanciaManhattan(avatar, g);

            if(distancia < menorDistancia){
                menorDistancia = distancia;
                posMasCercana = g;
            }
        }

        return posMasCercana;
    }

	/**
	 * Calcula la distnacia Manhattan entre dos posiciones del grid
	 * */
    static int distanciaManhattan (Vector2d src, Vector2d dest){
        return ((int) (Math.abs(src.x - dest.x) + Math.abs(src.y - dest.y)));
    }

	/**
	* Calcula el nivel de peligro de una casilla, este nivel de peligro depende de 
	* la distancia a la que se encuentren los enemigos
	* 2 casillas -> Peligro = 2
	* 1 casillas -> Peligro = 3
	* @param posicion Posicion en la que se encuentra el avatar
	* @return Nivel de peligro
	 */
	//TODO Tener en cuenta la orientacion del avatar
	static int nivelPeligro(Vector2d posicion, StateObservation stateObs){
		int peligro = 0;
		if(stateObs.getNPCPositions() != null){
			ArrayList<Observation> enemigos = stateObs.getNPCPositions()[0];

			int distancia;

			for(Observation enemigo:enemigos){
				Vector2d posEnemigoGrid = new Vector2d(Math.floor(enemigo.position.x / fescala.x),
						Math.floor(enemigo.position.y / fescala.y));
				distancia = distanciaManhattan(posicion, posEnemigoGrid);

				if(distancia == 4)
					peligro += 3;
				else if(distancia == 3)
					peligro += 4;
				else if(distancia == 2)
					peligro += 5;
				else if(distancia == 1)
					peligro += 8;
				else
					peligro += 0;

				//Si hay pared alrededor de la posicion se le suma 1 al peligro
				ArrayList<Vector2d> casilla_cercanas = posicionesADistanciaDe(1, posicion);
				for(Vector2d casilla:casilla_cercanas){
					// Para todas las casillas alrededor de esa posicion comprobamos si hay pared
					if(getElement(casilla, stateObs) == 0)
						peligro++;
				}

			}
		}

		return peligro;
	}

	/** 
	 * Obtiene el id del elemento que está en la posición 'position'
	 * @param position posicion del grid
	 * @param stateObs Objeto del tipo StateObservation necesario para obtener
	 * los elementos del mapa
	 * @return id del elemento en esa posición. -1 si no hay nada.
	 * */
	static int getElement(Vector2d position, StateObservation stateObs){
		if(((position.x < stateObs.getObservationGrid().length) && (position.x >= 0)) &&
		   ((position.y < stateObs.getObservationGrid()[0].length && (position.y >= 0))))
		{
			ArrayList<Observation> aux = stateObs.getObservationGrid()[(int)position.x][(int)position.y];

			if(!aux.isEmpty()){
				return aux.get(0).itype;
			}

			return -1;
		}
		else
			return 0;




	}

	/**
	* Busca una casilla que no esté en peligro alrededor del avatar
	* @return
	 */
	//TODO Si estamos en el �ltimo nivel coger la casilla mas cerca al objetivo
	public Vector2d buscarCasillaSegura(StateObservation stateObs){
		Vector2d casilla_segura = new Vector2d(avatar);
		boolean hay_peligro = true;
		int distancia_de_busqueda = 1;
		
		while(hay_peligro){
			ArrayList<Vector2d> posiciones_cercanas = posicionesADistancia(distancia_de_busqueda);
			int indice_posicion = 0;
			while(hay_peligro && indice_posicion < posiciones_cercanas.size()){
				casilla_segura = posiciones_cercanas.get(indice_posicion);
				if(((casilla_segura.x < stateObs.getObservationGrid().length) && (casilla_segura.x >= 0)) &&
				   ((casilla_segura.y < stateObs.getObservationGrid()[0].length && (casilla_segura.y >= 0)))){

					int elemento = getElement(casilla_segura, stateObs);
					if(nivelPeligro(casilla_segura, stateObs) == 0 && elemento != 0 &&
					   elemento != 10 && elemento != 11)
						hay_peligro = false;
				}
				indice_posicion++;
			}
			distancia_de_busqueda++;
		}

		return casilla_segura;
	}

	/**
	* Devuelve una lista de posiciones que esten a una distancia dada del
	* avatar
	* @param distancia
	* @return
	 */
	public ArrayList<Vector2d> posicionesADistancia(int distancia){
		ArrayList<Vector2d> posiciones = new ArrayList<>();

		for(int i = 0; i <= distancia; ++i){
			posiciones.add(new Vector2d(avatar.x + i, avatar.y + distancia-i));
			posiciones.add(new Vector2d(avatar.x + (-1)*i, avatar.y + distancia-i));
			posiciones.add(new Vector2d(avatar.x + i, avatar.y + (-1)*(distancia-i)));
			posiciones.add(new Vector2d(avatar.x + (-1)*i, avatar.y + (-1)*(distancia-i)));
		}

		return posiciones;
	}

	static ArrayList<Vector2d> posicionesADistanciaDe(int distancia, Vector2d posicion){
		ArrayList<Vector2d> posiciones = new ArrayList<>();

		for(int i = 0; i <= distancia; ++i){
			posiciones.add(new Vector2d(posicion.x + i, posicion.y + distancia-i));
			posiciones.add(new Vector2d(posicion.x + (-1)*i, posicion.y + distancia-i));
			posiciones.add(new Vector2d(posicion.x + i, posicion.y + (-1)*(distancia-i)));
			posiciones.add(new Vector2d(posicion.x + (-1)*i, posicion.y + (-1)*(distancia-i)));
		}

		return posiciones;
	}


	/**
	* Pasa de coordenadas del mapa a coordenadas del grid
	* @param position posicion en coordenadas del mapa
	* @return posicion en coordenadas del grid
	 */
	static Vector2d getGridPosition(Vector2d position){
		Vector2d grid_position = new Vector2d();
		grid_position.x = Math.floor(position.x / fescala.x);
		grid_position.y = Math.floor(position.y / fescala.y);

		return grid_position;
	}
}








