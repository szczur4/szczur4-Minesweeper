package szczur4.common;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
public class Stats extends File{
	public long flags,cleared,lost;
	public Stats(){super("world"+separator+"stats.json");}
	public void reset(){
		flags=0;
		cleared=0;
		lost=0;
	}
	public Stats load()throws IOException{
		if(!exists())return this;
		InputStreamReader isr=new InputStreamReader(new GZIPInputStream(new FileInputStream(this)));
		JsonObject root=new Gson().fromJson(isr,JsonObject.class);
		isr.close();
		flags=root.get("mines").getAsLong();
		cleared=root.get("cleared").getAsLong();
		lost=root.get("lost").getAsLong();
		return this;
	}
	public void save()throws IOException{new JsonWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(this)))).beginObject().name("mines").value(flags).name("cleared").value(cleared).name("lost").value(lost).endObject().close();}
}