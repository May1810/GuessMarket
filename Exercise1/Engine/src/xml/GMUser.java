package xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class GMUser {

    @XmlAttribute(name = "name")
    private String name;

    @XmlElement(name = "initial-cash")
    private double initialCash;

    @XmlElementWrapper(name = "GM-market-maker")
    @XmlElement(name = "event")
    private List<GMMarketMakerEvent> marketMakerEvents;

    public String getName() { return name; }
    public double getInitialCash() { return initialCash; }
    public List<GMMarketMakerEvent> getMarketMakerEvents() { return marketMakerEvents; }
}
