package workcraft.gate;

import java.util.UUID;

import workcraft.Document;
import workcraft.UnsupportedComponentException;
import workcraft.editor.BasicEditable;
import workcraft.util.Vec2;
import workcraft.visual.LineMode;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;
import workcraft.visual.shapes.Bezier;
import workcraft.visual.shapes.CompoundPath;
import workcraft.visual.shapes.Shape;
import workcraft.visual.shapes.Vertex;

public class AndGate extends BasicGate {
	public static final UUID _modeluuid = UUID.fromString("6f704a28-e691-11db-8314-0800200c9a66");
	public static final String _displayname = "AND gate";
	
	private Shape shape = null;
	
	public AndGate(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		min_inputs = 2;
		min_outputs = 1;
		max_outputs = 1;
		initContacts();
	}
	
	private Shape createGateShape() {
		CompoundPath p = new CompoundPath();
		p.addElement(new Vertex(-rad*0.8f, -rad));
		p.addElement(new Bezier(new Vec2(-rad*0.15f, -rad), new Vec2((rad)*0.4f, -rad),
			new Vec2(rad*0.9f, -(rad)*0.5f), new Vec2(rad*0.9f, 0.0f) ));
		p.addElement(new Bezier(new Vec2(rad*0.9f, 0.0f), new Vec2(rad*0.9f, (rad)*0.5f),
			new Vec2((rad)*0.4f, rad), new Vec2(-rad*0.15f, rad) ));
		p.addElement(new Vertex(-rad*0.8f, rad));
		p.setClosed(true);
		return new Shape(p);
	}
	
	public void doDraw(Painter p) {
		super.doDraw(p);
		p.setTransform(transform.getLocalToViewMatrix());
		p.setShapeMode(ShapeMode.FILL_AND_OUTLINE);
		p.setLineMode(LineMode.SOLID);
		p.setLineWidth(0.0075f);
		
		p.setLineColor((selected)?selectedOutlineColor:(canWork)?userOutlineColor:outlineColor);
		p.setFillColor((selected)?selectedFillColor:(canFire)?enabledFillColor:(lostSignal)?lostSignalFillColor:fillColor);
		p.drawShape(shape);

		// draw invertion circles
		for(GateContact c : in) {
			c.doDrawInvert(p);
		}
		out.getFirst().doDrawInvert(p);
	}
	
	protected void updateContactOffsets() {
		super.updateContactOffsets();
		shape = createGateShape();
		for(GateContact c : in) {
			c.offsInvX = rad*0.8f;
		}
		out.getFirst().offsInvX = -rad*0.9f;
	}
	
	public void refresh() {
		GateContact.StateType res = GateContact.StateType.set;
		for(GateContact cin : in) {
			if(cin.getState()==GateContact.StateType.reset) {
				res = GateContact.StateType.reset;
				break;
			}
		}
		out.getFirst().setState(res);
		super.refresh();
	}

	public Boolean isSet() {
		return (out.getFirst().getState()==GateContact.StateType.set);
	}
	
	public String getSetFunction() {
		boolean first = true;
		String s = "";
		for(GateContact c : in) {
			if(!first)
				s += " & ";
			first = false;
			s += c.getSrcFunction("ZERO");
		}
		
		if (out.getFirst().getInvertSignal())
			s = "not ("+s+")";
		
		return s;
	}

	public String getResetFunction() {
		boolean first = true;
		String s = "";
		for(GateContact c : in) {
			if(!first)
				s += " | ";
			first = false;
			s += "not "+c.getSrcFunction("ONE");
		}
		
		if (out.getFirst().getInvertSignal())
			s = "not ("+s+")";
		
		return s;
	}
}
