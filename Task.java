import java.util.ArrayList;
/**
 * An instance represents a process.
 * @author yashjalan
 *
 */
public class Task {
	//holds all req objects
	ArrayList<Request> req;
	//holds all rel objects
	ArrayList<Release> rel;
	//holds all resource objects
	ArrayList<InitiateResource> inRes;
	//holds the order (eg. req,rel,etc.)
	ArrayList<String> order;
	ArrayList<Integer> compute;
	int id;
	boolean aborted;
	int waitTime;
	int finishTime;
	int reqCount;
	int computeCount;
	int relCount;
	int computeCycles;
	int orderCount;
	boolean isComputing;
	//**//
	boolean inAborted;
	//**//
	
	Task(int id, int resourceType, int claim) {
		this.id = id;
		inRes = new ArrayList<InitiateResource>();
		inRes.add(new InitiateResource(resourceType, claim));
		req = new ArrayList<Request>();
		rel = new ArrayList<Release>();
		compute = new ArrayList<Integer>();
		order = new ArrayList<String>();
	}
	
}

/**
 * Represents a request object.
 * @author yashjalan
 *
 */
class Request {
	int resourceType;
	int num;
	
	Request(int resourceType, int num) {
		this.num = num;
		this.resourceType = resourceType;
	}
	
}

/**
 * Represents a release object.
 * @author yashjalan
 *
 */
class Release {
	int resourceType;
	int num;
	
	Release(int resourceType, int num) {
		this.num = num;
		this.resourceType = resourceType;
	}
}

/**
 * Represents a resource.
 * @author yashjalan
 *
 */
class InitiateResource implements Comparable<InitiateResource> {
	int resourceType;
	int claim;
	
	InitiateResource(int resourceType, int claim) {
		this.resourceType = resourceType;
		this.claim = claim;
	}

	@Override
	public int compareTo(InitiateResource o) {
		if (this.resourceType < o.resourceType) {
			return -1;
		} else if (this.resourceType > o.resourceType) {
			return 1;
		} else {
			return 0;
		}
	}
}