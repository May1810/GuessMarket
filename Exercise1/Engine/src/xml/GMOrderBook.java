package xml;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class GMOrderBook {
    @XmlAttribute(name = "allow-mint", required = true)
    protected boolean allowMint;

    @XmlAttribute(name = "initial", required = true)
    protected int initial;

    @XmlAttribute(name = "d", required = true)
    protected double d;

    public boolean isAllowMint() {
        return allowMint;
    }

    public int getInitial() {
        return initial;
    }

    public double getD() {
        return d;
    }
}