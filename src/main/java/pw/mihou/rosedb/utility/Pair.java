package pw.mihou.rosedb.utility;

public class Pair<T, Y> {

    private final T left;
    private final Y right;

    public Pair(T left, Y right){
        this.left = left;
        this.right = right;
    }

    public T getLeft(){
        return left;
    }

    public Y getRight(){
        return right;
    }

}
