package workcraft.sdfs;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.media.opengl.GL;

import org.python.core.PyObject;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.DuplicateIdException;
import workcraft.Document;
import workcraft.DocumentBase;
import workcraft.UnsupportedComponentException;
import workcraft.WorkCraftServer;
import workcraft.common.DefaultConnection;
import workcraft.editor.BasicEditable;
import workcraft.util.Colorf;
import workcraft.util.Mat4x4;
import workcraft.util.Vec2;
import workcraft.visual.LineMode;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;
import workcraft.visual.VertexFormat;
import workcraft.visual.PrimitiveType;
import workcraft.visual.VertexFormatException;

public abstract class SDFSLogic2Way extends SDFSLogicBase {
	private LogicState forward_state = LogicState.RESET;
	private LogicState backward_state = LogicState.RESET;

	
	protected boolean can_work_fwd = false;
	protected boolean can_work_back = false;

	private int fwd_eval_delay = 300;
	private int fwd_reset_delay = 300;

	private int back_eval_delay = 300;
	private int back_reset_delay = 300;

	private int fwd_event_start;
	private int back_event_start;

	private float fwd_event_progress = 0.0f; 
	private float back_event_progress = 0.0f;

	private static final Colorf reset = new Colorf (1.0f, 1.0f, 1.0f, 1.0f);
	private static final Colorf frameColor = new Colorf (0.0f, 0.0f, 0.0f, 1.0f);
	private static final Colorf selectedColor = new Colorf (0.5f, 0.0f, 0.0f, 1.0f);
	private static final Colorf evaluated = new Colorf ( 0.6f, 0.6f, 1.0f, 1.0f);	

	protected String fwdEvalFunc; //= "";
	protected String backEvalFunc;// = "";
	protected String fwdResetFunc;// = "";
	protected String backResetFunc;// = "";

	protected boolean[] funcEdited; 


	public List<String> getEditableProperties() {
		List<String> list = super.getEditableProperties();
		list.add("int,Forward eval delay,getFwdEvalDelay,setFwdEvalDelay");
		list.add("int,Forward reset delay,getFwdResetDelay,setFwdResetDelay");
		list.add("int,Backward eval delay,getBackEvalDelay,setBackEvalDelay");
		list.add("int,Backward reset delay,getBackResetDelay,setBackResetDelay");
		list.add("bool,FW enabled,getForwardState_ed,setForwardState");
		list.add("bool,BW enabled,getBackwardState_ed,setBackwardState");
		list.add("str,Fwd Eval,getFwdEvalFunc,setFwdEvalFunc");
		list.add("str,Back Eval,getBackEvalFunc,setBackEvalFunc");
		list.add("str,Fwd Reset,getFwdResetFunc,setFwdResetFunc");
		list.add("str,Back Reset,getBackResetFunc,setBackResetFunc");
		return list;
	}

	public void setForwardState(Boolean state) {
		if (state)
			this.forward_state = LogicState.EVALUATED;
		else
			this.forward_state = LogicState.RESET;
	}

	public void setBackwardState(Boolean state) {
		if (state)
			this.backward_state = LogicState.EVALUATED;
		else
			this.backward_state = LogicState.RESET;

	}

	public Boolean getBackwardState_ed() {
		return (backward_state == LogicState.EVALUATED);		
	}

	public Boolean getForwardState_ed() {
		return (forward_state == LogicState.EVALUATED);		
	}



	public SDFSLogic2Way(BasicEditable parent)  throws UnsupportedComponentException {
		super(parent);
		funcEdited = new boolean[4];
		for (int i=0; i<4; i++)
			funcEdited[i] = false;
		boundingBox.setExtents(new Vec2(-0.05f, -0.05f), new Vec2(0.05f, 0.05f));
	}

	@Override
	public void draw(Painter p) {

		p.setTransform(transform.getLocalToViewMatrix());

		p.setShapeMode(ShapeMode.FILL);
		p.setLineMode(LineMode.HAIRLINE);

		p.setFillColor(selectedColor);

		if (selected) {
			p.setFillColor(selectedColor);
			p.setLineColor(selectedColor);
		}
		else {
			if (highlight && ((DocumentBase)ownerDocument).isShowHighlight()) {
				p.setFillColor(((DocumentBase)ownerDocument).getHighlightColor());
				p.setLineColor(((DocumentBase)ownerDocument).getHighlightColor());
			}
			else {
				p.setFillColor(frameColor);
				p.setLineColor(frameColor);
			}
		}

		Colorf c;

		p.drawRect(-0.05f, 0.05f, 0.05f, -0.05f);

		switch (forward_state) {
		case EVALUATED:
			p.setFillColor(evaluated);
			break;
		case EVALUATING:
			c = Colorf.lerp( evaluated, reset, fwd_event_progress);
			p.setFillColor(c);
			break;
		case RESETTING:
			c = Colorf.lerp( reset, evaluated, fwd_event_progress);
			p.setFillColor(c);
			break;
		case RESET:
			p.setFillColor(reset);
			break;
		}

		p.drawRect(-0.045f, 0.045f, 0.045f, 0.0f);

		switch (backward_state) {
		case EVALUATED:
			p.setFillColor(evaluated);
			break;
		case EVALUATING:
			c = Colorf.lerp( evaluated, reset, back_event_progress);
			p.setFillColor(c);
			break;
		case RESETTING:
			c = Colorf.lerp( reset, evaluated, back_event_progress);
			p.setFillColor(c);
			break;
		case RESET:
			p.setFillColor(reset);
			break;
		}		

		p.drawRect(-0.045f, 0.0f, 0.045f, -0.045f);
		p.drawLine(-0.05f, 0.0f, 0.05f, 0.0f);

		super.draw(p);
	}


	@Override
	public BasicEditable getChildAt(Vec2 point) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void update(Mat4x4 matView) {
		// TODO Auto-generated method stub
	}

	public void fromXmlDom(Element element) throws DuplicateIdException {
		NodeList nl = element.getElementsByTagName("sdfs-logic-2way");
		Element te = (Element) nl.item(0);
		fwd_eval_delay = Integer.parseInt(te.getAttribute("fwd-eval-delay"));
		fwd_reset_delay = Integer.parseInt(te.getAttribute("fwd-reset-delay"));
		back_eval_delay = Integer.parseInt(te.getAttribute("back-eval-delay"));
		back_reset_delay = Integer.parseInt(te.getAttribute("back-reset-delay"));
		fwdEvalFunc = te.getAttribute("fwd-eval-func");
		fwdResetFunc = te.getAttribute("fwd-reset-func");
		backEvalFunc = te.getAttribute("back-eval-func");
		backResetFunc = te.getAttribute("back-reset-func");

		for (int i=0; i<4; i++)
			funcEdited[i] = Boolean.parseBoolean(te.getAttribute("func-edited-"+i));

		super.fromXmlDom(element);
	}

	public Element toXmlDom(Element parent_element) {
		Element ee = super.toXmlDom(parent_element); 
		org.w3c.dom.Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("sdfs-logic-2way");
		ppe.setAttribute("fwd-eval-delay", Integer.toString(fwd_eval_delay));
		ppe.setAttribute("fwd-reset-delay", Integer.toString(fwd_reset_delay));
		ppe.setAttribute("back-eval-delay", Integer.toString(back_eval_delay));
		ppe.setAttribute("back-reset-delay", Integer.toString(back_reset_delay));
		ppe.setAttribute("fwd-eval-func", fwdEvalFunc);
		ppe.setAttribute("back-eval-func", backEvalFunc);
		ppe.setAttribute("fwd-reset-func", fwdResetFunc);
		ppe.setAttribute("back-reset-func", backResetFunc);

		for (int i=0; i<4; i++)
			ppe.setAttribute("func-edited-"+i, Boolean.toString(funcEdited[i]));

		ee.appendChild(ppe);
		return ee;
	}

	public boolean simTick(int time_ms) {
		WorkCraftServer server = ownerDocument.getServer();
		SDFSModelBase doc = (SDFSModelBase)ownerDocument;

		boolean stateChanged = false;

		switch (forward_state) {
		case RESET:
			boolean can_evaluate = server.python.eval(fwdEvalFunc).__nonzero__();
			if (can_evaluate) {
				if ( (doc.isUserInteractionEnabled() && can_work_fwd) || !doc.isUserInteractionEnabled() ) {
					forward_state = LogicState.EVALUATING;
					fwd_event_progress = 0.0f;
					fwd_event_start = time_ms;
					can_evaluate = false;
					can_work_fwd = false;
				}
			}
			break;
		case RESETTING:
			if (time_ms > fwd_event_start+fwd_reset_delay) {
				forward_state = LogicState.RESET;
				doc.addTraceEvent(this.getId()+"/fwd_reset");
				stateChanged = true;
			}
			else 
				fwd_event_progress = (float)(time_ms - fwd_event_start) / (float)(fwd_reset_delay);
			break;
		case EVALUATING:
			if (time_ms > fwd_event_start+fwd_eval_delay) {
				forward_state = LogicState.EVALUATED;
				doc.addTraceEvent(this.getId()+"/fwd_eval");
				stateChanged = true;
			}
			else 
				fwd_event_progress = (float)(time_ms - fwd_event_start) / (float)(fwd_eval_delay);
			break;
		case EVALUATED:
			if ( (doc.isUserInteractionEnabled() && can_work_fwd) || !doc.isUserInteractionEnabled() ) {
				boolean can_reset = server.python.eval(fwdResetFunc).__nonzero__();
				if (can_reset) {
					forward_state = LogicState.RESETTING;
					fwd_event_progress = 0.0f;
					fwd_event_start = time_ms;
					can_work_fwd = false;
				}
			}
			break;
		}

		switch (backward_state) {
		case RESET:
			// check postset
			boolean can_evaluate = server.python.eval(backEvalFunc).__nonzero__();
			if (can_evaluate) {
				if ( (doc.isUserInteractionEnabled() && can_work_back) || !doc.isUserInteractionEnabled() ) {
					backward_state = LogicState.EVALUATING;
					back_event_progress = 0.0f;
					back_event_start = time_ms;
					can_evaluate = false;
					can_work_back = false;
				}
			}
			break;
		case RESETTING:
			if (time_ms > back_event_start+getBackResetDelay()) {
				backward_state = LogicState.RESET;
				doc.addTraceEvent(this.getId()+"/back_reset");
				stateChanged = true;
			}
			else 
				back_event_progress = (float)(time_ms - back_event_start) / (float)(getBackResetDelay());
			break;
		case EVALUATING:
			if (time_ms > back_event_start+back_eval_delay) {
				backward_state = LogicState.EVALUATED;
				doc.addTraceEvent(this.getId()+"/back_eval");
				stateChanged = true;
			}
			else 
				back_event_progress = (float)(time_ms - back_event_start) / (float)(back_eval_delay);
			break;
		case EVALUATED:
			boolean can_reset = server.python.eval(backResetFunc).__nonzero__();
			if (can_reset) {
				if ( (doc.isUserInteractionEnabled() && can_work_back) || !doc.isUserInteractionEnabled() ) {
					backward_state = LogicState.RESETTING;
					back_event_progress = 0.0f;
					back_event_start = time_ms;
					can_work_back = false;
				}
			}
			break;
		}

		return stateChanged;
	}


	public void setFwdEvalDelay(Integer fwd_eval_delay) {
		this.fwd_eval_delay = fwd_eval_delay;

		if (this.fwd_eval_delay < 0)
			this.fwd_eval_delay = 0;
		if (this.fwd_eval_delay > 5000)
			this.fwd_eval_delay = 5000;
	}

	public Integer getFwdEvalDelay() {
		return fwd_eval_delay;
	}

	public void setFwdResetDelay(Integer fwd_reset_delay) {
		this.fwd_reset_delay = fwd_reset_delay;
		if (this.fwd_reset_delay < 0)
			this.fwd_reset_delay = 0;
		if (this.fwd_reset_delay > 5000)
			this.fwd_reset_delay = 5000;

	}

	public Integer getFwdResetDelay() {
		return fwd_reset_delay;
	}

	public void setBackEvalDelay(Integer back_eval_delay) {
		this.back_eval_delay = back_eval_delay;

		if (this.back_eval_delay < 0)
			this.back_eval_delay = 0;
		if (this.back_eval_delay > 5000)
			this.back_eval_delay = 5000;
	}

	public Integer getBackEvalDelay() {
		return back_eval_delay;
	}

	public void setBackResetDelay(Integer back_reset_delay) {
		this.back_reset_delay = back_reset_delay;
		if (this.back_reset_delay < 0)
			this.back_reset_delay = 0;
		if (this.back_reset_delay > 5000)
			this.back_reset_delay = 5000;
	}

	public Integer getBackResetDelay() {
		return back_reset_delay;
	}

	public void setForwardState(LogicState forward_state) {
		this.forward_state = forward_state;
	}

	public LogicState getForwardState() {
		return forward_state;
	}

	public Boolean isForwardEvaluated() {
		return forward_state == LogicState.EVALUATED;
	}
	public Boolean isForwardReset() {
		return forward_state == LogicState.RESET;
	}
	public Boolean isBackwardEvaluated() {
		return backward_state == LogicState.EVALUATED;
	}
	public Boolean isBackwardReset() {
		return backward_state == LogicState.RESET;
	}

	public void setBackwardState(LogicState backward_state) {
		this.backward_state = backward_state;
	}

	public LogicState getBackwardState() {
		return backward_state;
	}

	public void setFwdEvalFunc(String fwdEvalFunc) {
		this.fwdEvalFunc = fwdEvalFunc;
		funcEdited[0] = true;
	}

	public String getFwdEvalFunc() {
		return fwdEvalFunc;
	}

	public void setBackEvalFunc(String backEvalFunc) {
		this.backEvalFunc = backEvalFunc;
		funcEdited[1] = true;
	}

	public String getBackEvalFunc() {
		return backEvalFunc;
	}

	public void setFwdResetFunc(String fwdResetFunc) {
		this.fwdResetFunc = fwdResetFunc;
		funcEdited[2] = true;
	}

	public String getFwdResetFunc() {
		return fwdResetFunc;
	}

	public void setBackResetFunc(String backResetFunc) {
		this.backResetFunc = backResetFunc;
		funcEdited[3] = true;
	}

	public String getBackResetFunc() {
		return backResetFunc;
	}


	public void clearFuncEditedFlags() {
		for (int i=0;i<4;i++)
			funcEdited[i] = false;
	}

	@Override
	public void rebuildRuleFunctions() {
		// TODO Auto-generated method stub

	}

	@Override
	public void restoreState(Object object) {
		forward_state = ((LogicState[])object)[0];
		backward_state = ((LogicState[])object)[1];
	}

	@Override
	public Object saveState() {
		LogicState[] s = new LogicState[2];
		s[0] = forward_state;
		s[1] = backward_state;
		return s;
	}

	public void simAction(int flag) {
		
		
		
		if (flag == MouseEvent.BUTTON1) {
			can_work_fwd  = true;
		} else if  (flag == MouseEvent.BUTTON3) {
			can_work_back = true;
		}
	}
}