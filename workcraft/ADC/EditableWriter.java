package workcraft.ADC;

import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.media.opengl.GL;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.DuplicateIdException;
import workcraft.Model;
import workcraft.UnsupportedComponentException;
import workcraft.common.DefaultConnection;
import workcraft.editor.BasicEditable;
import workcraft.editor.EditableConnection;
import workcraft.util.Colorf;
import workcraft.util.Mat4x4;
import workcraft.util.Vec2;
import workcraft.visual.JOGLPainter;
import workcraft.visual.LineMode;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;
import workcraft.visual.TextAlign;
import workcraft.visual.VertexBuffer;
import workcraft.visual.GeometryUtil;
import workcraft.visual.VertexFormat;
import workcraft.visual.PrimitiveType;
import workcraft.visual.VertexFormatException;

public class EditableWriter extends BasicEditable {
	public static final UUID _modeluuid = UUID.fromString("3c60dee8-0545-11dc-8314-0800200c9a66");
	public static final String _displayname = "Writer";

	private static Colorf writerColor = new Colorf(1.0f, 1.0f, 1.0f, 1.0f);
	private static Colorf selectedWriterColor = new Colorf(1.0f, 0.9f, 0.9f, 1.0f);
	private static Colorf writerOutlineColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);
	private static Colorf selectedWriterOutlineColor = new Colorf(0.5f, 0.0f, 0.0f, 1.0f);

	private static Colorf enabledWriterColor = new Colorf(0.6f, 0.6f, 1.0f, 1.0f);
	private static Colorf userWriterOutlineColor = new Colorf(0.0f, 0.6f, 0.0f, 1.0f);

	public boolean canFire = false;
	public boolean canWork = false;	
	
	public ADCToken token = null;	
	private LinkedList<EditableACM> out;
	private LinkedList<EditableProcess> in;

	public LinkedList<EditableACM> getOut()
	{
		return (LinkedList<EditableACM>)out.clone();
	}
	
	public LinkedList<EditableProcess> getIn()
	{
		return (LinkedList<EditableProcess>)in.clone();
	}

	public void removeOut(EditableACM t)
	{
		out.remove(t);
	}

	public void removeIn(EditableProcess t)
	{
		in.remove(t);
	}

	public boolean addIn(DefaultConnection con)
	{
		EditableProcess t = (EditableProcess)con.getFirst();
		if (in.contains(t))
			return false;
		in.add(t);
		connections.add(con);
		return true;
	}
	
	public boolean addOut(DefaultConnection con)
	{
		EditableACM t = (EditableACM)con.getSecond();
		if (out.contains(t))
			return false;
		out.add(t);
		connections.add(con);
		return true;
	}

	public EditableWriter(BasicEditable parent) throws UnsupportedComponentException
	{
		super(parent);
		boundingBox.setExtents(new Vec2(-0.05f, -0.05f), new Vec2(0.05f, 0.05f));
		out = new LinkedList<EditableACM>();
		in = new LinkedList<EditableProcess>();
	}
	
	public void draw(Painter p)
	{
		
		p.setTransform(transform.getLocalToViewMatrix());
		p.setShapeMode(ShapeMode.FILL);

		if (selected)
			p.setFillColor(selectedWriterOutlineColor);
		else
			if (canWork)
				p.setFillColor(userWriterOutlineColor);
			else
				p.setFillColor(writerOutlineColor);
		
		p.drawRect(-0.05f, 0.05f, 0.05f, -0.05f);		

		
		if (selected)
			p.setFillColor(selectedWriterColor);
		else
			if (canFire)
				p.setFillColor(enabledWriterColor);
			else
				p.setFillColor(writerColor);
		
		p.drawRect(-0.04f, 0.04f, 0.04f, -0.04f);
		
		if (selected)
			p.setLineColor(selectedWriterOutlineColor);
		else
			p.setLineColor(writerOutlineColor);

		p.setLineMode(LineMode.SOLID);
		p.setLineWidth(0.004f);
		p.drawLine(0.005f, 0.02f, 0.035f, 0.02f);
		p.drawLine(0.02f, 0.005f, 0.02f, 0.035f);
		
		super.draw(p);
	}

	@Override
	public BasicEditable getChildAt(Vec2 point)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void update(Mat4x4 matView)
	{
		// TODO Auto-generated method stub

	}

	public List<String> getEditableProperties()
	{
		List<String> list = super.getEditableProperties();
		//list.add("int,Color,getColor,setColor");
		return list;
	}


	public void fromXmlDom(Element element) throws DuplicateIdException
	{
		NodeList nl = element.getElementsByTagName("writer");
		//Element ne = (Element) nl.item(0);
		//setColor(Integer.parseInt(ne.getAttribute("color")));
		super.fromXmlDom(element);
	}
	
	public Element toXmlDom(Element parent_element)
	{
		Element ee = super.toXmlDom(parent_element);
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("writer");
		//ppe.setAttribute("color", Integer.toString(getColor()));
		ee.appendChild(ppe);
		return ee;
	}
			
	public void simAction(int flag) 
	{
		if (flag == MouseEvent.BUTTON1)
		{
			canWork = !canWork;
		}
	}

}