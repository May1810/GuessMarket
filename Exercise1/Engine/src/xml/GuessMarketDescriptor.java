package xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "Guess-Market")
@XmlAccessorType(XmlAccessType.FIELD)
public class GuessMarketDescriptor {

    @XmlElementWrapper(name = "GM-events")
    @XmlElement(name = "GM-event")
    private List<GMEvent> events;

    public List<GMEvent> getEvents() {
        return events;
    }

}
