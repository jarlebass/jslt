
package com.schibsted.spt.data.jstl2.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.schibsted.spt.data.jstl2.JstlException;

/**
 * Indexing and slicing of arrays and also strings.
 */
public class ArraySlicer extends AbstractNode {
  private ExpressionNode left;
  private boolean colon;
  private ExpressionNode right;
  private ExpressionNode parent;

  public ArraySlicer(ExpressionNode left, boolean colon, ExpressionNode right,
                     ExpressionNode parent, Location location) {
    super(location);
    this.left = left;
    this.colon = colon;
    this.right = right;
    this.parent = parent;
  }

  public JsonNode apply(Scope scope, JsonNode input) {
    JsonNode sequence = parent.apply(scope, input);
    if (!sequence.isArray() && !sequence.isTextual())
      return NullNode.instance;

    int size = sequence.size();
    if (sequence.isTextual())
      size = sequence.asText().length();

    int leftix = resolveIndex(scope, left, input, size, 0);
    if (!colon) {
      if (sequence.isArray()) {
        JsonNode val = sequence.get(leftix);
        if (val == null)
          val = NullNode.instance;
        return val;
      } else {
        String string = sequence.asText();
        if (leftix >= string.length())
          throw new JstlException("String index out of range: " + leftix, location);
        return new TextNode("" + string.charAt(leftix));
      }
    }

    int rightix = resolveIndex(scope, right, input, size, size);
    if (rightix > size)
      rightix = size;

    if (sequence.isArray()) {
      ArrayNode result = NodeUtils.mapper.createArrayNode();
      for (int ix = leftix; ix < rightix; ix++)
        result.add(sequence.get(ix));
      return result;
    } else {
      String string = sequence.asText();
      return new TextNode(string.substring(leftix, rightix));
    }
  }

  private int resolveIndex(Scope scope, ExpressionNode expr,
                           JsonNode input, int size, int ifnull) {
    if (expr == null)
      return ifnull;

    JsonNode node = expr.apply(scope, input);
    if (!node.isNumber())
      throw new JstlException("Can't index array/string with " + node, location);

    int ix = node.intValue();
    if (ix < 0)
      ix = size + ix;
    return ix;
  }

  public void dump(int level) {
    if (parent != null)
      parent.dump(level);
    System.out.println(NodeUtils.indent(level) + this);
  }

  public String toString() {
    return "[" + left + " : " + right + "]";
  }
}
