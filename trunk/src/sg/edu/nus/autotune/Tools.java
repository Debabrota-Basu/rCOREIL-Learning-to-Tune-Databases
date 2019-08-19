package sg.edu.nus.autotune;

import java.util.List;


public class Tools {
	//check if o1 is covered by o2.
	public static boolean IsCovered(List<Integer> o1, List<Integer> o2){
		if(o1.size()>o2.size()){
			return false;
		}else{
			return o1.equals(o2.subList(0, o1.size()));
		}
	}
	
	//check if o1 is covering o2.
	public static boolean IsCovering(List<Integer> o1, List<Integer> o2){
		return IsCovered(o2,o1);
	}
}
