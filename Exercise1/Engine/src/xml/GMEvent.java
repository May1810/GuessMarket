package xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class GMEvent {

    @XmlAttribute(name = "name")
    private String name;

    @XmlElement(name = "id")
    private int id;

    @XmlElement(name = "description")
    private String description;

    @XmlElement(name = "comision")
    private Commission commission;

    @XmlElementWrapper(name = "GM-options")
    @XmlElement(name = "GM-option")
    private List<String> options;

    @XmlElement(name = "GM-method")
    private GMMethod method;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Commission getCommission() { return commission; }
    public List<String> getOptions() { return options; }
    public GMMethod getMethod() { return method; }
}