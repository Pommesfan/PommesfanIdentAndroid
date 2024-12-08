package utils;

import java.util.Vector;

public class Observable<T> {
    private final Vector<Observer<T>> v;

    /** Construct an Observable with zero Observers. */

    public Observable() {
        v = new Vector<>();
    }

    public void addObserver(Observer<T> o) {
        if (o == null)
            throw new NullPointerException();
        if (!v.contains(o)) {
            v.addElement(o);
        }
    }

    public synchronized void deleteObserver(Observer<T> o) {
        v.removeElement(o);
    }


    public void notifyObservers(T t) {
        for (int i = v.size() - 1; i >= 0; i--)
            v.get(i).update(t);
    }

}
