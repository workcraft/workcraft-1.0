package workcraft.stg;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import workcraft.DuplicateIdException;
import workcraft.Tool;
import workcraft.ToolType;
import workcraft.WorkCraftServer;
import workcraft.editor.BasicEditable;
import workcraft.editor.Editor;
import workcraft.petri.EditablePetriPlace;
import workcraft.petri.EditablePetriTransition;
import workcraft.petri.GFileFilter;

public class STGDotGSaver implements Tool {
	public static final String _modeluuid = "10418180-D733-11DC-A679-A32656D89593";
	public static final String _displayname = "Export to .g format";
	
	class STGConnection implements Comparable<STGConnection> {
		String key;
		Set<String> values = new HashSet<String>();
		
		STGConnection (String key, String value) {
			this.key = key;
			values.add(value);
		}
		
		public int compareTo(STGConnection c) {
			return this.key.compareTo(c.key);
		}
	}
	
	public boolean writeFile (String path, STGModel doc) throws IOException {
		
		PrintWriter out = new PrintWriter(new FileWriter(path));
		out.print("# STG file generated by Workcraft.\n");
		
		
		// update transition types
		doc.updateTransitionTypes();

		//////////////////////////////////////////////////
		// create the lists of all of the transition types
		ArrayList<String> internal	= new ArrayList<String>();
		ArrayList<String> inputs	= new ArrayList<String>();
		ArrayList<String> outputs	= new ArrayList<String>();
		ArrayList<String> dummy		= new ArrayList<String>();
		
		LinkedList<EditablePetriTransition> transitions	= new LinkedList<EditablePetriTransition>();
		
		
		doc.getTransitions(transitions);
		
		// assign transition IDs to remove _copy_ stuff
		int tnum=1;
		for (BasicEditable be: transitions) {
			if (be.getId().contains("copy")) {
				while (doc.getServer().python.get("t"+tnum)!=null) {
					tnum++;
				}
				
				try {
					be.setId("t"+tnum);
				} catch (DuplicateIdException e) {
					e.printStackTrace();
				}
			}
		}
		
		

		
		
		Pattern p = Pattern.compile(STGModel.signalPattern);
		String l;
		
		LinkedList<BasicEditable> labeled = new LinkedList<BasicEditable>();
		
		// sort out all the transitions
		for (BasicEditable be: transitions) {
			l = be.getLabel();

			Matcher m = p.matcher(l); // is label?
			
			// work with all transitions except dummies
			if (!l.equals("")&&!l.equals("dummy")) {
				int t = ((EditableSTGTransition)be).getTransitionType();
				if (m.find()) {
					switch (t) {
					case 0:
						if (!internal.contains(m.group(1))) internal.add(m.group(1));
						break;
					case 1:
						if (!inputs.contains(m.group(1))) inputs.add(m.group(1));
						break;
					case 2:
						if (!outputs.contains(m.group(1))) outputs.add(m.group(1));
						break;
					case 3:
						//error !
						System.err.println("Unexpected dummy transition found: "+l);
					}
				} else {
					System.err.println("Transition '"+l+"' is not recognized!");
				}
			}
		}
		
		// process all the dummies, assign label names to them as well
		int dnum=0;
//		out.print("# Dummy names: ");
		for (BasicEditable be: transitions) {
			if (be.getLabel().equals("") || be.getLabel().equals("dummy")) {
				// get a new dummy label available
				l="";
				while (l.equals("")) {
					++dnum;
					l="d"+dnum;
					if (inputs.contains(l)||outputs.contains(l)||internal.contains(l)) l=""; // get another try
				}
				be.setLabel(l);
//				out.print(l+"'");
				labeled.add(be);
				dummy.add(l);
			}
		}
//		out.println();
		
		// process all the places, assign label names to them as well
		LinkedList<EditablePetriPlace> places = new LinkedList<EditablePetriPlace>();
		doc.getPlaces(places);
		
		int pnum=0;// start with p0
		// debug place names
//		out.print("# Place names: ");
		for (BasicEditable be: places) {
			if (be.getLabel().equals("") ) {
				// get a new place label available
				l="";
				while (l.equals("")) {
					l="p"+pnum;
					pnum++;
					if (inputs.contains(l)||outputs.contains(l)||internal.contains(l)) l=""; // get another try
				}
				labeled.add(be);
//				out.print(l+"'");
				be.setLabel(l);
			}
		}
//		out.println();
		
		Collections.sort(inputs);
		Collections.sort(outputs);
		Collections.sort(internal);
		Collections.sort(dummy);
		
		////////////////////////////////////////////////////
		// prepare connection lists

		List<String> connections1 = new ArrayList<String>(); // connections from transitions
		List<String> connections2 = new ArrayList<String>(); // connections from places
		String tokens = ""; // all the tokens are separated with space
		
		for (EditablePetriTransition t : transitions) {
			List<String> ts = new ArrayList<String>();
			for (BasicEditable be : t.getOut()) {
				ts.add(be.getLabel());
			}
			Collections.sort(ts);
			
			String ts2 = "";
			for (String s: ts) ts2+=" "+s;
			connections1.add(t.getLabel()+ts2);
		}

		for (EditablePetriPlace c :  places) {
			List<String> ts = new ArrayList<String>();
			for (BasicEditable be : c.getOut()) {
				ts.add(be.getLabel());
			}
			Collections.sort(ts);
			
			String ts2 = "";
			for (String s: ts) ts2+=" "+s;
			connections2.add(c.getLabel()+ts2);

			if (c.getTokens()>0) {
				tokens+=" "+c.getLabel();
				if (c.getTokens()>1)
					tokens+="="+c.getTokens();
			}
		}
		
		////////////////////////////////////////////////////
		// save everything now
		if (internal.size()>0) {
			Collections.sort(internal);
			out.print(".internal ");
			for (String s : internal) out.print(" "+s);
			out.print("\n");
		}

		if (inputs.size()>0) {
			Collections.sort(inputs);
			out.print(".inputs ");
			for (String s : inputs) out.print(" "+s);
			out.print("\n");
		}
		
		if (outputs.size()>0) {
			Collections.sort(outputs);
			out.print(".outputs ");
			for (String s : outputs) out.print(" "+s);
			out.print("\n");
		}
		
		if (dummy.size()>0) {
			Collections.sort(dummy);
			out.print(".dummy ");
			for (String s : dummy) out.print(" "+s);
			out.print("\n");
		}
		
		out.print(".graph\n");

		Collections.sort(connections1);
		for (String s : connections1) out.print(s+"\n");
		
		Collections.sort(connections2);
		for (String s : connections2) out.print(s+"\n");

		out.print(".marking { "+tokens+" }\n");
		out.print(".end\n");
		out.close();
		
		
		
		// need to preserve used identifiers
		for (BasicEditable be: places) {
			if (!be.getLabel().equals(""))
				try {
					be.setId("__"+be.getId()+"_temp");
				} catch (DuplicateIdException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		
		for (BasicEditable be: places) {
			if (!be.getLabel().equals(""))
				try {
					be.setId(be.getLabel());
				} catch (DuplicateIdException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}

		// return empty labels
		for (BasicEditable be: labeled) {
//			if (be instanceof EditableSTGPlace) continue;
		
			be.setLabel("");
/*			if (be instanceof EditableSTGTransition) {
				int t = ((EditableSTGTransition)be).getTransitionType(); 
				if (t==3) {
					be.setLabel("dummy");
				}
			}*/
		}
		
		return true;
	}


	public void deinit(WorkCraftServer server) {
		// TODO Auto-generated method stub
		
	}

	public ToolType getToolType() {
		// TODO Auto-generated method stub
		return  ToolType.EXPORT;
	}

	public void init(WorkCraftServer server) {
		// TODO Auto-generated method stub
		
	}

	public boolean isModelSupported(UUID modelUuid) {
		// TODO Auto-generated method stub
		return false;
	}

	public void run(Editor editor, WorkCraftServer server) {
		STGModel doc = (STGModel) (editor.getDocument());
		String last_directory = editor.getLastDirectory();

		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new GFileFilter());
		if (last_directory != null)
			fc.setCurrentDirectory(new File(last_directory));
		if (fc.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
		{
			String path = fc.getSelectedFile().getPath();
			if (!path.endsWith(".g")) path += ".g";
			{
				// saving in .g format
				try
				{
					writeFile(path, doc);
				}
				catch (IOException e)
				{
					JOptionPane.showMessageDialog(null, "File could not be opened for writing.");
					return;
				}
			}
		}
		
	}

}
