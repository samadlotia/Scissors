package scissors.internal;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;

class ScissorsPanel implements CytoPanelComponent {
  public Component getComponent() {
    return new JPanel();
  }

  public CytoPanelName getCytoPanelName() {
    return CytoPanelName.WEST;
  }

  public Icon getIcon() {
    return new ImageIcon(this.getClass().getResource("/icon.png"));
  }

  public String getTitle() {
    return "Scissors ";
  }
}