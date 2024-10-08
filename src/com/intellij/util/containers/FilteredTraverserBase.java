/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util.containers;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.intellij.openapi.util.Conditions.not;

public abstract class FilteredTraverserBase<T, Self extends FilteredTraverserBase<T, Self>> implements Iterable<T> {

  protected final Meta<T> meta;
  protected final Function<T, ? extends Iterable<? extends T>> tree;

  protected FilteredTraverserBase(@Nullable Meta<T> meta, @NotNull Function<T, ? extends Iterable<? extends T>> tree) {
    this.tree = tree;
    this.meta = meta == null ? Meta.<T>empty() : meta;
  }

  @NotNull
  public final T getRoot() {
    return meta.roots.iterator().next();
  }

  @NotNull
  public final Iterable<? extends T> getRoots() {
    return meta.roots;
  }

  @Override
  public final Iterator<T> iterator() {
    return traverse().iterator();
  }

  @NotNull
  protected abstract Self newInstance(Meta<T> meta);

  @NotNull
  public final JBIterable<T> traverse(@NotNull TreeTraversal traversal) {
    Function<T, Iterable<? extends T>> adjusted = new Function<T, Iterable<? extends T>>() {
      @Override
      public Iterable<? extends T> fun(T t) {
        return children(t);
      }
    };
    return traversal.traversal(getRoots(), adjusted).filter(meta.filter.AND);
  }

  @NotNull
  public final JBIterable<T> traverse() {
    return traverse(meta.traversal);
  }

  @NotNull
  public final JBIterable<T> preOrderDfsTraversal() {
    return traverse(TreeTraversal.PRE_ORDER_DFS);
  }

  @NotNull
  public final JBIterable<T> postOrderDfsTraversal() {
    return traverse(TreeTraversal.POST_ORDER_DFS);
  }

  @NotNull
  public final JBIterable<T> bfsTraversal() {
    return traverse(TreeTraversal.PLAIN_BFS);
  }

  @NotNull
  public final JBIterable<T> tracingBfsTraversal() {
    return traverse(TreeTraversal.TRACING_BFS);
  }

  /**
   * Clears expand, regard and filter conditions, traversal while keeping roots and "forced" properties.
   *  FilteredTraverserBase#forceIgnore(Condition)
   *  FilteredTraverserBase#forceDisregard(Condition)
   */
  @NotNull
  public final Self reset() {
    return newInstance(meta.reset());
  }

  @NotNull
  public final Self withRoot(@Nullable T root) {
    return newInstance(meta.withRoots(ContainerUtil.createMaybeSingletonList(root)));
  }

  @NotNull
  public final Self withRoots(@NotNull Iterable<? extends T> roots) {
    return newInstance(meta.withRoots(roots));
  }

  @NotNull
  public final Self withTraversal(TreeTraversal type) {
    return newInstance(meta.withTraversal(type));
  }

  /**
   * Restricts the nodes that can have children by the specified condition.
   * Subsequent calls will AND all the conditions.
   */
  @NotNull
  public final Self expand(@NotNull Condition<? super T> c) {
    return newInstance(meta.expand(c));
  }

  /**
   * Restricts the nodes that can be children by the specified condition while <b>keeping the edges</b>.
   * Subsequent calls will AND all the conditions.
   */
  @NotNull
  public final Self regard(@NotNull Condition<? super T> c) {
    return newInstance(meta.regard(c));
  }

  @NotNull
  public final Self expandAndFilter(Condition<? super T> c) {
    return newInstance(meta.expand(c).filter(c));
  }

  @NotNull
  public final Self expandAndSkip(Condition<? super T> c) {
    return newInstance(meta.expand(c).filter(not(c)));
  }

  @NotNull
  public final Self filter(@NotNull Condition<? super T> c) {
    return newInstance(meta.filter(c));
  }

  @NotNull
  public final <C> JBIterable<C> filter(@NotNull Class<C> type) {
    return traverse().filter(type);
  }

  /**
   * Converts the configured traversal to skip already visited nodes.
   *  TreeTraversal#unique()
   */
  @NotNull
  public final Self unique() {
    return withTraversal(meta.traversal.unique());
  }

  /**
   * Converts the configured traversal to skip already visited nodes.
   *  TreeTraversal#unique(Function)
   */
  @NotNull
  public final Self unique(@NotNull Function<T, Object> identity) {
    return withTraversal(meta.traversal.unique(identity));
  }

  /**
   * Excludes the nodes by the specified condition from any traversal completely.
   *  FilteredTraverserBase#expand(Condition)
   *  FilteredTraverserBase#filter(Condition)
   *  FilteredTraverserBase#reset()
   */
  @NotNull
  public final Self forceIgnore(@NotNull Condition<? super T> c) {
    return newInstance(meta.forceIgnore(c));
  }

  /**
   * Excludes the nodes by the specified condition while keeping their edges.
   *  FilteredTraverserBase#regard(Condition)
   *  FilteredTraverserBase#reset()
   */
  @NotNull
  public final Self forceDisregard(@NotNull Condition<? super T> c) {
    return newInstance(meta.forceDisregard(c));
  }

  /**
   * Returns the children of the specified node as seen by this traverser.
   *  TreeTraversal.TracingIt to obtain parents of a node during a traversal
   */
  @NotNull
  public final JBIterable<T> children(@Nullable T node) {
    if (node == null || isAlwaysLeaf(node)) {
      return JBIterable.empty();
    }
    else if (meta.regard.next == null && meta.forceDisregard.next == null) {
      return (JBIterable<T>) JBIterable.from(tree.fun(node)).filter(not(meta.forceIgnore.OR));
    }
    else {
      // traverse subtree to select accepted children
      return TreeTraversal.GUIDED_TRAVERSAL(meta.createChildrenGuide(node)).traversal(node, tree);
    }
  }

  protected boolean isAlwaysLeaf(@NotNull T node) {
    return !meta.expand.valueAnd(node);
  }

  @NotNull
  public final List<T> toList() {
    return traverse().toList();
  }

  @NotNull
  public final Set<T> toSet() {
    return traverse().toSet();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
           "traversal=" + meta.traversal +
           '}';
  }

  public abstract static class EdgeFilter<T> extends JBIterable.StatefulFilter<T> {

    protected T edgeSource;

  }

  @SuppressWarnings("unchecked")
  protected static class Meta<T> {
    final Iterable<? extends T> roots;
    final TreeTraversal traversal;
    final Cond<T> expand;
    final Cond<T> regard;
    final Cond<T> filter;

    final Cond<T> forceIgnore;
    final Cond<T> forceDisregard;

    public Meta(@NotNull Iterable<? extends T> roots,
                @NotNull TreeTraversal traversal,
                @NotNull Cond<T> expand,
                @NotNull Cond<T> regard,
                @NotNull Cond<T> filter,
                @NotNull Cond<T> forceIgnore,
                @NotNull Cond<T> forceDisregard) {
      this.roots = roots;
      this.traversal = traversal;
      this.expand = expand;
      this.regard = regard;
      this.filter = filter;
      this.forceIgnore = forceIgnore;
      this.forceDisregard = forceDisregard;
    }

    public Meta<T> reset() {
      return new Meta<T>(roots, TreeTraversal.PRE_ORDER_DFS, Cond.TRUE, Cond.TRUE, Cond.TRUE, forceIgnore, forceDisregard);
    }

    public Meta<T> withRoots(@NotNull Iterable<? extends T> roots) {
      return new Meta<T>(roots, traversal, expand, regard, filter, forceIgnore, forceDisregard);
    }

    public Meta<T> withTraversal(TreeTraversal traversal) {
      return new Meta<T>(roots, traversal, expand, regard, filter, forceIgnore, forceDisregard);
    }

    public Meta<T> expand(@NotNull Condition<? super T> c) {
      return new Meta<T>(roots, traversal, expand.append(c), regard, this.filter, forceIgnore, forceDisregard);
    }

    public Meta<T> regard(@NotNull Condition<? super T> c) {
      return new Meta<T>(roots, traversal, expand, regard.append(c), this.filter, forceIgnore, forceDisregard);
    }

    public Meta<T> filter(@NotNull Condition<? super T> c) {
      return new Meta<T>(roots, traversal, expand, regard, this.filter.append(c), forceIgnore, forceDisregard);
    }

    public Meta<T> forceIgnore(Condition<? super T> c) {
      return new Meta<T>(roots, traversal, expand, regard, this.filter, forceIgnore.append(c), forceDisregard);
    }

    public Meta<T> forceDisregard(Condition<? super T> c) {
      return new Meta<T>(roots, traversal, expand, regard, this.filter, forceIgnore, forceDisregard.append(c));
    }

    TreeTraversal.GuidedIt.Guide<T> createChildrenGuide(final T parent) {
      return new TreeTraversal.GuidedIt.Guide<T>() {
        final Condition<? super T> expand = buildExpandConditionForChildren(parent);
        @Override
        public void guide(@NotNull TreeTraversal.GuidedIt<T> guidedIt) {
          doPerformChildrenGuidance(guidedIt, expand);
        }
      };
    }

    private void doPerformChildrenGuidance(TreeTraversal.GuidedIt<T> it, Condition<? super T> expand) {
      if (it.curChild == null) return;
      if (forceIgnore.valueOr(it.curChild)) return;
      if (it.curParent == null || expand.value(it.curChild)) {
        it.queueNext(it.curChild);
      }
      else {
        it.result(it.curChild);
      }
    }

    private Condition<? super T> buildExpandConditionForChildren(T parent) {
      // implements: or2(forceExpandAndSkip, not(childFilter));
      // and handles JBIterable.StatefulTransform and EdgeFilter conditions
      Cond copy = null;
      boolean invert = true;
      Cond c = regard;
      while (c != null) {
        Condition impl = JBIterable.Stateful.copy(c.impl);
        if (impl != (invert ? Condition.TRUE : Condition.FALSE)) {
          copy = new Cond<Object>(invert ? not(impl) : impl, copy);
          if (impl instanceof EdgeFilter) {
            ((EdgeFilter)impl).edgeSource = parent;
          }
        }
        if (c.next == null) {
          c = invert ? forceDisregard : null;
          invert = false;
        }
        else {
          c = c.next;
        }
      }
      return copy == null ? Condition.FALSE : copy.OR;
    }

    private static final Meta<?> EMPTY = new Meta<Object>(
      JBIterable.empty(), TreeTraversal.PRE_ORDER_DFS,
      Cond.TRUE, Cond.TRUE, Cond.TRUE,
      Cond.FALSE, Cond.FALSE);

    public static <T> Meta<T> empty() {
      return (Meta<T>)EMPTY;
    }

  }

  private static class Cond<T> {
    final static Cond TRUE = new Cond<Object>(Conditions.TRUE, null);
    final static Cond FALSE = new Cond<Object>(Conditions.FALSE, null);

    final Condition<? super T> impl;
    final Cond<T> next;

    Cond(Condition<? super T> impl, Cond<T> next) {
      this.impl = impl;
      this.next = next;
    }

    Cond<T> append(Condition<? super T> impl) {
      return new Cond<T>(impl, this);
    }

    boolean valueAnd(T t) {
      for (Cond<T> c = this; c != null; c = c.next) {
        if (!c.impl.value(t)) return false;
      }
      return true;
    }

    boolean valueOr(T t) {
      for (Cond<T> c = this; c != null; c = c.next) {
        if (c.impl.value(t)) return true;
      }
      return false;
    }

    final Condition<? super T> OR = new Condition<T>() {
      @Override
      public boolean value(T t) {
        return valueOr(t);
      }
    };

    final Condition<? super T> AND = new Condition<T>() {
      @Override
      public boolean value(T t) {
        return valueAnd(t);
      }
    };

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("Cond{");
      for (Cond<T> c = this; c != null; c = c.next) {
        sb.append(JBIterator.toShortString(c.impl));
        if (c.next != null) sb.append(", ");
      }
      return sb.append("}").toString();
    }
  }

}
