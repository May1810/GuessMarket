package xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
public class Commission {

    @XmlAttribute(name = "type")
    private String type;

    @XmlValue
    private int value;

    public String getType() { return type; }
    public int getValue() { return value; }
}