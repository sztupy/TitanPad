package scot.raven.titanpad.core.evdev;

public class InputEvent extends IInputEvent.Stub {
    private long time = 0;
    private int eventType = 0;
    private int eventCode = 0;
    private int eventValue = 0;

    @Override
    public void destroy() {}

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public int getEventType() {
        return eventType;
    }

    @Override
    public int getEventCode() {
        return eventCode;
    }

    @Override
    public int getEventValue() {
        return eventValue;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setEventType(int eventType) {
        this.eventType = eventType;
    }

    public void setEventCode(int eventCode) {
        this.eventCode = eventCode;
    }

    public void setEventValue(int eventValue) {
        this.eventValue = eventValue;
    }

    @Override
    public String toString() {
        return "InputEvent{" +
                "time=" + time +
                ", eventType=" + eventType +
                ", eventCode=" + eventCode +
                ", eventValue=" + eventValue +
                '}';
    }
}