package xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class GMMarketMakerEvent {

    @XmlAttribute(name = "id")
    private int id;

    public int getId() { return id; }
}
