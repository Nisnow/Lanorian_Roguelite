package editor;

import java.io.File;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import graphics.Animation;

public class JSONFileGenerator 
{
	private File jsonFile;
	private AnimationList animationList;
	private String name;
	
	public void setFile(File p_file)
	{
		jsonFile = p_file;
	}
	
	public void setAnimationList(AnimationList p_list)
	{
		animationList = p_list;
	}
	
	public void setName(String p_name)
	{
		name = p_name;
	}
	
	/*
	 * Reference:
	 * https://www.mkyong.com/java/json-simple-example-read-and-write-json/
	 */
	@SuppressWarnings("unchecked")
	public void generate()
	{
		JSONObject obj = new JSONObject();
		
		// Store all animation data in a JSON Array
		JSONArray list = new JSONArray();
		for(int i = 0; i < animationList.getSize(); i++)
		{
			JSONObject animObj = new JSONObject();
			Animation a = animationList.getElementAt(i);
			
			animObj.put("name", a.getName());
			animObj.put("x", a.getFrame().x+""); //convert to string
			animObj.put("y", a.getFrame().y+"");
			animObj.put("w", a.getFrame().w+"");
			animObj.put("h", a.getFrame().h+"");
			
			if(a.getFrameCount() != 0)
				animObj.put("frames", a.getFrameCount()+"");
			
			if(a.getInterval() != 0)
				animObj.put("interval", a.getInterval()+"");
			
			if(a.isLoop())
				animObj.put("loop", "true");
		}
		
		obj.put("animations", list);
		//if file exists, overwrite
		//else, generate new file and set jsonFile to this
	}
}
