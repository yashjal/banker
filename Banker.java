import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
/**
 * Implements the banker's algorithm and FIFO.
 * Please check README.txt to learn how to run.
 * @author yashjalan
 *
 */
public class Banker {	

	public static void main(String[] args) throws FileNotFoundException {
		
		Scanner input = new Scanner(new File(args[0]));
		input.useDelimiter("\\s*(\\n|\\s)\\s*");
		
		//reading input all at once
		int nT = input.nextInt();
		int nR = input.nextInt();
		// holds all resources
		int[] nRes = new int[nR];
		
		for (int i=0; i<nR; i++) {
			nRes[i] = input.nextInt();
		}
		
		Task[] t = new Task[nT];
		
		while (input.hasNext()) {
				String s = input.next();
				
				if (s.equals("initiate")) {
					int id = input.nextInt();
					if (t[id-1] != null) {
						t[id-1].inRes.add(new InitiateResource(input.nextInt(), input.nextInt()));
						t[id-1].order.add(s);
					} else {
						t[id-1] = new Task(id, input.nextInt(), input.nextInt());
						t[id-1].order.add(s);
					}
					
				} else if (s.equals("request")) {
					int id = input.nextInt();
					t[id-1].req.add(new Request(input.nextInt(), input.nextInt()));
					t[id-1].order.add(s);
					
				} else if (s.equals("release")) {
					int id = input.nextInt();
	
					t[id-1].rel.add(new Release(input.nextInt(), input.nextInt()));
					t[id-1].order.add(s);
					
				} else if (s.equals("compute")) {
					int id = input.nextInt();

					t[id-1].compute.add(input.nextInt());
					t[id-1].order.add(s);
					input.nextInt();
					
				} else if (s.equals("terminate")) {
					t[input.nextInt()-1].order.add(s);
					input.nextInt(); input.nextInt();
					
				}
		}
		
		//sort the resource arraylist of each process
		for (int i=0; i<t.length; i++) {
			Collections.sort(t[i].inRes);
		}
		
		if (args.length == 1) {
			opManager(t, nRes);
			System.out.println("FIFO");
		} else if (args.length == 2) {
			if (args[1].equalsIgnoreCase("fifo")) {
				opManager(t, nRes);
				System.out.println("FIFO");
			} else if (args[1].equalsIgnoreCase("banker")) {
				banker(t, nRes);
				System.out.println("Banker's Alg.");
			}
		}

		int totalTime = 0;
		int totalWaitTime = 0;
		
		for (int i=0; i<t.length; i++) {
			System.out.print("Task"+(i+1)+"      ");
			if (t[i].aborted) {
				System.out.println("aborted");
			} else {
				System.out.print(t[i].finishTime+"    "+t[i].waitTime
						+"    "+t[i].waitTime*100/t[i].finishTime+"%\n");
				totalTime += t[i].finishTime;
				totalWaitTime += t[i].waitTime;
			}
		}
		System.out.println("total      "+totalTime+"    " +totalWaitTime+"    "+totalWaitTime*100/totalTime+"%\n");	
		
		input.close();
	}
	
	/**
	 * Implements bankers algorithm.
	 * @param t array of processes
	 * @param nRes array of resources
	 */
	public static void banker(Task[] t, int[] nRes) {
		
		int cycle = 0;
		int[] iHave = new int[nRes.length];
		iHave = nRes;
		int[][] matrix = new int[t.length][2*nRes.length];
			
		//check if initial claim > resources
		for (int i=0; i<t.length; i++) {
			for (int j=0; j<nRes.length; j++) {
				if (t[i].inRes.get(j).claim > nRes[t[i].inRes.get(j).resourceType-1]) {
					t[i].aborted = true;
					t[i].inAborted = true;
					System.out.println("initial claim > resources present");
				}
			}
		}
		
		//setup matrix with each row as one process
		//and each column as either claim or has
		for (int i=0; i<t.length; i++) {
			for (int j=0; j<2*nRes.length; j++) {
				if (t[i].aborted) {
					matrix[i][j] = 0;
				} else {
					if (j<nRes.length) {
						matrix[i][j] = t[i].inRes.get(j).claim;
					} else {
						matrix[i][j] = 0;
					}
				}
			}
		}
		
		//fifo data structure
		Queue<Integer> pending = new LinkedList<Integer>();
		
		while(true) {
			//check if all tasks have terminated
			int count = 0;
			int[] releases = new int[t.length];
			boolean[] blocked = new boolean[t.length];
			
			for (int i=0; i<t.length; i++) {
				if (t[i].aborted || t[i].finishTime != 0) {
					count++;
				}
			}
			if (count == t.length) {
				break;
			}
			
			int[][] tmpMatrix = new int[t.length][2*nRes.length];
			int[] tmpHave = new int[nRes.length];			
			
			Integer[] copy1 = new Integer[pending.size()];
			Integer[] copy = pending.toArray(copy1);
			pending.clear();

			//first check requests for blocked processes
			for (int g=0; g < copy.length; g++) {
				int h = copy[g];
				blocked[h] = true;
				Request req = t[h].req.get(t[h].reqCount);		
				
				tmpMatrix = new int[t.length][2*nRes.length];
				tmpHave = new int[nRes.length];
				for (int j = 0; j < matrix.length; j++) {
					System.arraycopy(matrix[j], 0, tmpMatrix[j], 0, matrix[j].length);
				}
				System.arraycopy(iHave,0,tmpHave,0,iHave.length);
				
				//pretend to grant it
				tmpMatrix[h][nRes.length+req.resourceType-1] += req.num;
				tmpHave[req.resourceType-1] -= req.num;	
				
				if (isSafe(tmpMatrix, tmpHave, t)) {	
					matrix[h][nRes.length+req.resourceType-1] += req.num;
					iHave[req.resourceType-1] -= req.num;		
					t[h].reqCount++;
					t[h].orderCount++;

				} else {
					pending.add(h);
					t[h].waitTime++;
				} 
			}
				 
			
			for (int i=0; i<t.length; i++) {
				
				if (blocked[i]) {
					continue;
				}
				
				if (t[i].isComputing) {
					if (t[i].computeCycles == 0) {
						t[i].isComputing = false;
					} else {
						t[i].computeCycles--;
					}
				}
				
				if (t[i].aborted || t[i].isComputing || t[i].finishTime != 0) {
					continue;
				}
				
				if (t[i].order.get(t[i].orderCount).equals("terminate")) {
					t[i].finishTime = cycle;
					t[i].orderCount++;
					continue;
				}
				
				if (t[i].order.get(t[i].orderCount).equals("request")) {
					Request req = t[i].req.get(t[i].reqCount);
					//check if request > claim
					if (req.num + matrix[i][nRes.length+(req.resourceType-1)] > 
						t[i].inRes.get(req.resourceType-1).claim) {
						t[i].aborted = true;
						System.out.println("request > claim");
						
						for (int k=0; k<nRes.length;k++) {
							int numRes = matrix[i][k+nRes.length];
							iHave[k] += numRes;
							matrix[i][k+nRes.length] = 0;
						}
						
					} else {
						tmpMatrix = new int[t.length][2*nRes.length];
						tmpHave = new int[nRes.length];
						
						for (int j = 0; j < matrix.length; j++) {
						  System.arraycopy(matrix[j], 0, tmpMatrix[j], 0, matrix[j].length);
						}
						System.arraycopy(iHave,0,tmpHave,0,iHave.length);
						
						//pretend to grant it
						tmpMatrix[i][nRes.length+req.resourceType-1] += req.num;
						tmpHave[req.resourceType-1] -= req.num;	
						
						if (isSafe(tmpMatrix, tmpHave, t)) {	
							matrix[i][nRes.length+req.resourceType-1] += req.num;
							iHave[req.resourceType-1] -= req.num;		
							t[i].reqCount++;
							t[i].orderCount++;

						} else {
							t[i].waitTime++;
							pending.add(new Integer(i));
						} 
					}
					
				} else if (t[i].order.get(t[i].orderCount).equals("release")) {
					releases[i] = i+1;
					
				} else if (t[i].order.get(t[i].orderCount).equals("compute")) {
					t[i].isComputing = true;
					t[i].computeCycles = t[i].compute.get(t[i].computeCount) - 1;
					t[i].computeCount++;
					t[i].orderCount++;
				} else if (t[i].order.get(t[i].orderCount).equals("initiate")) {
					t[i].orderCount++;
				}
				
			}
			
			//releases
			for (int i=0; i<t.length;i++) {
				if (releases[i] != 0) {
					Release rel = t[i].rel.get(t[i].relCount);
					iHave[rel.resourceType-1] += rel.num; 
					matrix[i][nRes.length+rel.resourceType-1] -= rel.num;
					t[i].relCount++;
					t[i].orderCount++;
				}
			}
			
			cycle++;
		}
		
	}
	
	/**
	 * Implements the FIFO method.
	 * @param t array of processes
	 * @param nRes array of resources
	 */
	public static void opManager(Task[] t, int[] nRes) {
		
		int cycle = 0;
		int[] iHave = new int[nRes.length];
		iHave = nRes;
		int[][] matrix = new int[t.length][nRes.length];
		
		//fifo data structure
		Queue<Integer> pending = new LinkedList<Integer>();
		
		while(true) {
			
			int[] releases = new int[t.length];
			int count = 0;
			boolean[] blocked = new boolean[t.length];
			
			//decide if all tasks have terminated
			for (int i=0; i<t.length; i++) {
				if (t[i].aborted || t[i].finishTime != 0) {
					count++;
				}
			}
			if (count == t.length) {
				break;
			}
			
			Integer[] copy1 = new Integer[pending.size()];
			Integer[] copy = pending.toArray(copy1);
			pending.clear();
			//first check for blocked processes
			for (int g=0; g < copy.length; g++) {
				int h = copy[g];
				blocked[h] = true;
				Request req = t[h].req.get(t[h].reqCount);

				if (req.num <= iHave[req.resourceType-1]) {
					matrix[h][req.resourceType-1] += req.num;
					iHave[req.resourceType-1] -= req.num;
					t[h].reqCount++;
					t[h].orderCount++;
				} else {
					t[h].waitTime++;
					pending.add(h);
				}
				
			} 
						
			for (int i=0; i<t.length; i++) {
				
				//if the process was in blocked state, continue
				if (blocked[i]) {
					continue;
				}
				
				if (t[i].isComputing) {
					if (t[i].computeCycles == 0) {
						t[i].isComputing = false;
					} else {
						t[i].computeCycles--;
					}
				}
				
				if (t[i].aborted || t[i].isComputing || t[i].finishTime != 0) {
					continue;
				}
				
				if (t[i].order.get(t[i].orderCount).equals("terminate")) {
					t[i].finishTime = cycle;
					t[i].orderCount++;
					continue;
				}
				
				if (t[i].order.get(t[i].orderCount).equals("request")) {
					
					Request req = t[i].req.get(t[i].reqCount);
					
					if (req.num <= iHave[req.resourceType-1]) {
						matrix[i][req.resourceType-1] += req.num;
						iHave[req.resourceType-1] -= req.num;
						t[i].reqCount++;
						t[i].orderCount++;
						
						// just in case if the process is blocked, should never go inside
						if (pending.contains(i)) {
							copy1 = new Integer[pending.size()];
							copy = pending.toArray(copy1);
							pending.clear();
							for (int l=0; l<copy.length; l++) {
								if ((int)(copy[l]) == i) {
									copy[l] = -1;
								} else {
									pending.add(copy[l]);
								}
							}
						}

					} else {
						pending.add(new Integer(i));
						t[i].waitTime++;
					}
					
				} else if (t[i].order.get(t[i].orderCount).equals("release")) {
					releases[i] = i+1;
					
				} else if (t[i].order.get(t[i].orderCount).equals("compute")) {
					t[i].isComputing = true;
					t[i].computeCycles = t[i].compute.get(t[i].computeCount) - 1;
					t[i].computeCount++;
					t[i].orderCount++;
				} else if (t[i].order.get(t[i].orderCount).equals("initiate")) {
					t[i].orderCount++;
				}
				
			}
			
			//releases
			for (int i=0; i<t.length;i++) {
				if (releases[i] != 0) {
					Release rel = t[i].rel.get(t[i].relCount);
					iHave[rel.resourceType-1] += rel.num; 
					matrix[i][rel.resourceType-1] -= rel.num;
					t[i].relCount++;
					t[i].orderCount++;
				}
			}
			
			int c = 0;
			//check for deadlock
			for (int j=0; j<t.length; j++) {
				if (t[j].aborted || t[j].finishTime != 0) {
						c++;
				}
			}
			int x = 0;
			if (t.length-c == pending.size()) {
				while(t[x].aborted || t[x].finishTime != 0) {
					x++;
					if (x >= t.length) {
						break;
					}
				}				
				
				while (x<=t.length-1 && t[x].reqCount < t[x].req.size() &&
						t[x].req.get(t[x].reqCount).num > iHave[t[x].req.get(t[x].reqCount).resourceType-1] ) {
					t[x].aborted = true;
					System.out.println("deadlock, so abort process" + x);
					for (int k=0; k<nRes.length;k++) {
						int numRes = matrix[x][k];
						iHave[k] += numRes;
						matrix[x][k] = 0;
					}
					copy1 = new Integer[pending.size()];
					copy = pending.toArray(copy1);
					pending.clear();
					for (int l=0; l<copy.length; l++) {
						if ((int)(copy[l]) == x) {
							copy[l] = -1;
						} else {
							pending.add(copy[l]);
						}
					}
					x++;
				}
				
				
			}
			
			cycle++;
		}
		
	}
	
	/**
	 * Checks if a state is safe.
	 * @param tmpMatrix matrix with each row as process and each column as claim or has resources
	 * @param tmpHave array with resources that resource manager has
	 * @return true if state is safe, false otherwise
	 */
	public static boolean isSafe(int[][] tmpMatrix, int[] tmpHave, Task[] t) {
		
		while (true) {
			int count = 0;
			int countP = 0;
			int countP2 = 0;
			
			//no process exists
			for (int i=0; i<tmpMatrix.length; i++) {

				if (tmpMatrix[i] == null) {
					countP2++;
				} 
				
			}
			if (countP2 == tmpMatrix.length) {
				return true;
			}
			
			
			for (int i=0; i<tmpMatrix.length; i++) {

				if (tmpMatrix[i] == null) {
					continue;
				}
				//**//
				if (t[i].inAborted) {
					tmpMatrix[i] = null;
					countP++;
					continue;
				}
				//**//
				
				count = 0;
				for (int j=0; j<tmpHave.length; j++) {
					if ((tmpMatrix[i][j] - tmpMatrix[i][j+tmpHave.length]) <= tmpHave[j]) {
						count++;
					}
				}
				if (count == tmpHave.length) {
					for (int k=0; k<tmpHave.length; k++) {
						int numRes = tmpMatrix[i][k+tmpHave.length];
						tmpHave[k] += numRes;
						tmpMatrix[i][k+tmpHave.length] -= numRes;
					}
					tmpMatrix[i] = null;
				} else {
					countP++;
				}
			}
			
			if (countP == tmpMatrix.length) {
				return false;
			}
			
		}
		
	}
	
}
