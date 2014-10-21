package soot.we.android.component;

import java.util.List;

import soot.we.android.MainThread;


public class LeakingPath {
	public EntitySinkInf sink;
	public List<EntitySourceInf> sourcepath;
	public LeakingPath(List<EntitySourceInf> scs,EntitySinkInf sk){
		  sourcepath = scs;
	      sink = sk;
	}
	public boolean equalTo(LeakingPath p) {
		if(!this.sink.getStmt().toString().equals(p.sink.getStmt().toString()))
			return false;
		if(!this.sourcepath.get(0).getStmt().toString().equals(p.sourcepath.get(0)
			.getStmt().toString()))
			return false;
		return true;
	}
	public boolean IsCorrectPath(String className) {
		// TODO Auto-generated method stub
		EntityClass tmpec = null;
		for(EntityClass ec : MainThread.classList){
			if(ec.getclassName().equals(className)){
				tmpec = ec;
				break;
			}
		}
		EntitySourceInf headsource= sourcepath.get(0);
		if(headsource.getStmt().toString().contains("android.content.Intent")){
				if(tmpec!=null&&!tmpec.isEntryPointClass())
						return false;
		}
		return true;
	}
}
