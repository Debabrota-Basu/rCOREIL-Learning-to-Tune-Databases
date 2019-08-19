package sg.edu.nus.autotune;

public class Query
{
    protected final long _id;
    protected final Object _content;

    public Query(long id, Object content) {
        _id = id;
        _content = content;
    }

    public long getId() {
        return _id;
    }

    public Object getContent() {
        return _content;
    }

    public boolean equals(Query that) {
        return (_id == that.getId() ? true : false);
    }

    @Override
    public String toString() {
        return ("q" + _id);
    }
}