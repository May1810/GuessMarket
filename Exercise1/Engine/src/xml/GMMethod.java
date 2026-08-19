package xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class GMMethod {

    @XmlElement(name = "GM-LMSR")
    private GMLmsr lmsr;

    public GMLmsr getLmsr() { return lmsr; }
}