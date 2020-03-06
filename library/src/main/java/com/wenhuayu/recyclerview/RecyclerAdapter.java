package com.wenhuayu.recyclerview;

import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * 一个通用的RecyclerView适配器
 * 用户可向内添加任意的Holder类型与数据
 * 适配器将自动管理Holder的实例化以及局部刷新
 * Created by WenHuayu<why94@qq.com> on 2017/11/27.
 */
@SuppressWarnings({"UnusedReturnValue", "WeakerAccess", "unused"})
public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.Holder> {

    @androidx.annotation.Nullable
    private final Object                                    mEnclosing;
    @androidx.annotation.NonNull
    private final SparseArray<HolderFactory>                mHolderFactories;
    @androidx.annotation.NonNull
    private final HashMap<Class<? extends Holder>, Integer> mHolder2Type;
    @androidx.annotation.NonNull
    private       ArrayList<ItemData>                       mAdapterData = new ArrayList<>();
    @androidx.annotation.Nullable
    private       ArrayList<ItemData>                       mTransactionData;

    /**
     * @param enclosing 列表项{@link Holder}的外部类,通过反射实例化时需要
     */
    public RecyclerAdapter(@androidx.annotation.Nullable Object enclosing) {
        mEnclosing = enclosing;
        mHolderFactories = new SparseArray<>();
        mHolder2Type = new HashMap<>();
    }

    /**
     * @return 是否不处于事务模式
     */
    private boolean notInTransaction() {
        return mTransactionData == null;
    }

    /**
     * @return 若适配器处于事务状态, 则返回事务数据, 若适配器不处于事务状态, 则返回适配器数据
     */
    private List<ItemData> data() {
        return notInTransaction() ? mAdapterData : mTransactionData;
    }

    /**
     * @return 适配器数据数量
     */
    @Override
    public int getItemCount() {
        return mAdapterData.size();
    }

    /**
     * @return 事务数据或适配器数据数量
     */
    public int getDataCount() {
        return data().size();
    }

    /**
     * @return 适配器数据指定下标处数据的类型
     */
    @Override
    public int getItemViewType(int position) {
        return mAdapterData.get(position).type;
    }

    /**
     * @return Holder对应的数据类型
     */
    public int getItemViewType(@androidx.annotation.NonNull Class<? extends Holder> holder) {
        Integer type = mHolder2Type.get(holder);
        if (type == null) {
            type = mHolder2Type.size();
            mHolder2Type.put(holder, type);
            mHolderFactories.put(type, new HolderFactory(mEnclosing, holder));
        }
        return type;
    }

    @androidx.annotation.NonNull
    @Override
    public Holder onCreateViewHolder(@androidx.annotation.NonNull ViewGroup parent, int type) {
        return mHolderFactories.get(type).create(parent);
    }

    @Override
    public void onBindViewHolder(@androidx.annotation.NonNull Holder holder, int position) {
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onBindViewHolder(@androidx.annotation.NonNull Holder holder, int position, @androidx.annotation.NonNull List<Object> payloads) {
        holder.onBindData(position, holder.data = mAdapterData.get(position).data, payloads);
    }

    @Override
    public void onViewAttachedToWindow(@androidx.annotation.NonNull Holder holder) {
        holder.onViewAttachedToWindow();
    }

    @Override
    public void onViewDetachedFromWindow(@androidx.annotation.NonNull Holder holder) {
        holder.onViewDetachedFromWindow();
    }

    @Override
    public void onViewRecycled(@androidx.annotation.NonNull Holder holder) {
        holder.onViewRecycled();
    }

    public <T> RecyclerAdapter add(Class<? extends Holder<T>> holder, T data) {
        data().add(ItemData.single(getItemViewType(holder), data));
        if (notInTransaction()) {
            notifyItemInserted(mAdapterData.size() - 1);
        }
        return this;
    }

    public <T> RecyclerAdapter add(int index, Class<? extends Holder<T>> holder, T data) {
        data().add(index, ItemData.single(getItemViewType(holder), data));
        if (notInTransaction()) {
            notifyItemInserted(index);
        }
        return this;
    }

    public <T> RecyclerAdapter add(Class<? extends Holder<T>> holder, List<T> data) {
        if (data != null && !data.isEmpty()) {
            data().addAll(ItemData.list(getItemViewType(holder), data));
            if (notInTransaction()) {
                notifyItemRangeInserted(mAdapterData.size() - data.size(), data.size());
            }
        }
        return this;
    }

    public <T> RecyclerAdapter add(int index, @androidx.annotation.NonNull Class<? extends Holder<T>> holder, List<T> data) {
        if (data != null && !data.isEmpty()) {
            data().addAll(index, ItemData.list(getItemViewType(holder), data));
            if (notInTransaction()) {
                notifyItemRangeInserted(index, data.size());
            }
        }
        return this;
    }

    public <T> RecyclerAdapter change(int index, Class<? extends Holder<T>> holder, T data, Object payload) {
        data().set(index, ItemData.single(getItemViewType(holder), data));
        if (notInTransaction()) {
            notifyItemChanged(index, payload);
        }
        return this;
    }

    public <T> RecyclerAdapter change(int index, Class<? extends Holder<T>> holder, T data) {
        return change(index, holder, data, null);
    }

    public RecyclerAdapter change(int from, int to, Object payload) {
        if (notInTransaction()) {
            notifyItemRangeChanged(from, to - from, payload);
        }
        return this;
    }

    public RecyclerAdapter change(int index, Object payload) {
        if (notInTransaction()) {
            notifyItemChanged(index, payload);
        }
        return this;
    }

    public RecyclerAdapter change(int index) {
        return change(index, null);
    }

    public RecyclerAdapter remove(int index) {
        data().remove(index);
        if (notInTransaction()) {
            notifyItemRemoved(index);
        }
        return this;
    }

    public RecyclerAdapter remove(int from, int to) {
        if (from != to) {
            data().subList(from, to).clear();
            if (notInTransaction()) {
                notifyItemRangeRemoved(from, to - from);
            }
        }
        return this;
    }

    public RecyclerAdapter clear() {
        return remove(0, data().size());
    }

    /**
     * [0,1,2] -> move(0,2) -> [2,1,0]
     * <p>
     * [meta(int,0),meta(int,1),meta(int,2)] -> move(0,2) -> [meta(int,2),meta(int,1),meta(int,0)]
     */
    public RecyclerAdapter move(int from, int to) {
        List<ItemData> metas = data();
        metas.add(to, metas.remove(from));
        if (notInTransaction()) {
            notifyItemMoved(from, to);
        }
        return this;
    }

    /**
     * 通知所有数据项检查payload并根据需要更新
     */
    public RecyclerAdapter notifyDataSetChanged(Object payload) {
        notifyItemRangeChanged(0, getItemCount(), payload);
        return this;
    }

    /**
     * @see RecyclerAdapter#beginTransaction(boolean)
     */
    public RecyclerAdapter beginTransaction() {
        return beginTransaction(true);
    }

    /**
     * 适配器进入事务模式,所有数据更改项均不会立即影响适配器的向外提供数据源,只有在提交事务后,才会统一更新数据
     *
     * @param cancelExistingTransaction 如果适配器正处于事务模式中,是否取消现有事务数据,如果不取消,则会在现有事务的基础上叠加改动
     */
    public RecyclerAdapter beginTransaction(boolean cancelExistingTransaction) {
        if (cancelExistingTransaction || mTransactionData == null) {
            mTransactionData = new ArrayList<>(mAdapterData);
        }
        return this;
    }

    public RecyclerAdapter cancelTransaction() {
        mTransactionData = null;
        return this;
    }

    public RecyclerAdapter commitTransaction(DifferenceComparator... differenceComparators) {
        return commitTransaction(true, differenceComparators);
    }

    /**
     * @param detectMoves @see DiffUtil#calculateDiff(DiffUtil.Callback, boolean)
     */
    public RecyclerAdapter commitTransaction(boolean detectMoves, DifferenceComparator... differenceComparators) {
        if (mTransactionData != null) {
            final SparseArray<DifferenceComparator<?>> comparators = new SparseArray<>(differenceComparators.length);
            for (DifferenceComparator<?> comparator : differenceComparators) {
                comparators.put(getItemViewType(comparator.holder), comparator);
            }
            final List<ItemData> newItems = mTransactionData;
            final List<ItemData> oldItems = mAdapterData;
            mAdapterData = mTransactionData;
            mTransactionData = null;
            DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return oldItems.size();
                }

                @Override
                public int getNewListSize() {
                    return newItems.size();
                }

                @Override
                @SuppressWarnings("unchecked")
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    ItemData oldItem = oldItems.get(oldItemPosition);
                    ItemData newItem = newItems.get(newItemPosition);
                    // 1
                    if (oldItem.type != newItem.type) {
                        return false;
                    }
                    // 2
                    DifferenceComparator comparator = comparators.get(newItem.type);
                    if (comparator != null) {
                        return comparator.areItemsTheSame(oldItem.data, newItem.data);
                    }
                    // 3
                    if (newItem.data instanceof Comparable) {
                        return ((Comparable) newItem.data).entity(oldItem.data);
                    }
                    // 4
                    return Objects.equals(oldItem.data, newItem.data);
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    return getChangePayload(oldItemPosition, newItemPosition) == null;
                }

                @Override
                @SuppressWarnings("unchecked")
                public Object getChangePayload(int oldItemPosition, int newItemPosition) {
                    ItemData oldItem = oldItems.get(oldItemPosition);
                    ItemData newItem = newItems.get(newItemPosition);
                    // 1
                    DifferenceComparator mComparator = comparators.get(newItem.type);
                    if (mComparator != null) {
                        return mComparator.getChangePayload(oldItem.data, newItem.data);
                    }
                    // 2
                    if (newItem.data instanceof Comparable) {
                        return ((Comparable) newItem.data).payload(oldItem.data);
                    }
                    // 3
                    return null;
                }
            }, detectMoves).dispatchUpdatesTo(this);
        }
        return this;
    }

    private static class ItemData {
        final int    type;
        final Object data;

        private ItemData(int type, Object data) {
            this.type = type;
            this.data = data;
        }

        static ItemData single(int type, Object data) {
            return new ItemData(type, data);
        }

        static List<ItemData> list(int type, List data) {
            List<ItemData> list = new ArrayList<>(data.size());
            for (Object o : data) {
                list.add(new ItemData(type, o));
            }
            return list;
        }
    }

    private static class HolderFactory {
        final Constructor<? extends Holder> constructor;
        final Class<? extends Holder>       holder;
        final Object[]                      args;

        HolderFactory(Object enclosing, Class<? extends Holder> holder) {
            if (holder.isMemberClass() && !Modifier.isStatic(holder.getModifiers())) {
                if (!Objects.requireNonNull(holder.getEnclosingClass()).isInstance(enclosing)) {
                    throw new RuntimeException(String.format("该适配器不支持非%s的非静态内部类:%s", enclosing.getClass().getName(), holder.getName()));
                }
                try {
                    this.constructor = holder.getConstructor(holder.getEnclosingClass(), ViewGroup.class);
                    this.args = new Object[]{enclosing, null};
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(String.format("请为%s提供构造方法(ViewGroup)", holder));
                }
            } else {
                try {
                    this.constructor = holder.getConstructor(ViewGroup.class);
                    this.args = new Object[]{null};
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(String.format("请为%s提供唯一构造方法(ViewGroup)", holder));
                }
            }
            this.constructor.setAccessible(true);
            this.holder = holder;
        }

        Holder create(ViewGroup parent) {
            args[args.length - 1] = parent;
            try {
                return constructor.newInstance(args);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            } finally {
                args[args.length - 1] = null;
            }
        }
    }

    public abstract static class Holder<T> extends RecyclerView.ViewHolder {
        private T data;

        @Deprecated
        @SuppressWarnings("ConstantConditions")
        public Holder(ViewGroup group) {
            super(null);
        }

        public Holder(ViewGroup group, View view) {
            super(view);
        }

        public Holder(ViewGroup group, @LayoutRes int layout) {
            super(LayoutInflater.from(group.getContext()).inflate(layout, group, false));
        }

        /**
         * @see RecyclerView.Adapter#onBindViewHolder(RecyclerView.ViewHolder, int, List)
         */
        protected  void onBindData(int position, T data, @androidx.annotation.NonNull List<Object> payloads) {
            switch (payloads.size()) {
                case 0:
                    onBindData(position, data, (Object) null);
                    onBindData(position, data);
                    break;
                case 1:
                    onBindData(position, data, payloads.get(0));
                    onBindData(position, data);
                    break;
                default:
                    for (Object payload : payloads) {
                        onBindData(position, data, payload);
                    }
                    onBindData(position, data);
                    break;
            }
        }

        /**
         * @see RecyclerView.Adapter#onBindViewHolder(RecyclerView.ViewHolder, int, List)
         */
        protected void onBindData(int position, T data, @androidx.annotation.Nullable Object payload) {
        }

        /**
         * @see RecyclerView.Adapter#onBindViewHolder(RecyclerView.ViewHolder, int, List)
         */
        protected void onBindData(int position, T data) {
        }

        /**
         * @see RecyclerView.Adapter#onViewAttachedToWindow(RecyclerView.ViewHolder)
         */
        protected void onViewAttachedToWindow() {
        }

        /**
         * @see RecyclerView.Adapter#onViewDetachedFromWindow(RecyclerView.ViewHolder)
         */
        protected void onViewDetachedFromWindow() {
        }

        /**
         * @see RecyclerView.Adapter#onViewRecycled(RecyclerView.ViewHolder)
         */
        protected void onViewRecycled() {
        }

        protected final T getData() {
            return data;
        }

        protected final <V extends View> V view(@IdRes int id) {
            return itemView.findViewById(id);
        }

        protected final boolean payload(Object payload) {
            return payload == null;
        }

        protected final boolean payload(Object payload, int flag) {
            return payload == null || (payload instanceof Integer && ((int) payload & flag) == flag);
        }
    }

    public interface Comparable<T> {
        boolean entity(T who);

        Object payload(T old);
    }

    /**
     * @see DiffUtil.Callback
     */
    public static abstract class DifferenceComparator<T> {
        private final Class<? extends Holder<T>> holder;

        public DifferenceComparator(Class<? extends Holder<T>> holder) {
            this.holder = holder;
        }

        /**
         * @see DiffUtil.Callback#areItemsTheSame(int, int)
         */
        protected boolean areItemsTheSame(T oldItem, T newItem) {
            if (newItem instanceof Comparable) {
                //noinspection unchecked
                return ((Comparable) newItem).entity(oldItem);
            }
            return Objects.equals(oldItem, newItem);
        }

        /**
         * @see DiffUtil.Callback#areContentsTheSame(int, int)
         */
        protected boolean areContentsTheSame(T oldItem, T newItem) {
            if (newItem instanceof Comparable) {
                //noinspection unchecked
                return ((Comparable) newItem).payload(oldItem) == null;
            }
            return getChangePayload(oldItem, newItem) == null;
        }

        /**
         * @see DiffUtil.Callback#getChangePayload(int, int)
         */
        protected Object getChangePayload(T oldItem, T newItem) {
            if (newItem instanceof Comparable) {
                //noinspection unchecked
                return ((Comparable) newItem).payload(oldItem);
            }
            return null;
        }
    }
}