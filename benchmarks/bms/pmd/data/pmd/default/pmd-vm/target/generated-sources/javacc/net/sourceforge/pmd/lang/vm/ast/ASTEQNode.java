/* Generated By:JJTree: Do not edit this line. ASTEQNode.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=true,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package net.sourceforge.pmd.lang.vm.ast;

public
class ASTEQNode extends net.sourceforge.pmd.lang.vm.ast.AbstractVmNode {
  public ASTEQNode(int id) {
    super(id);
  }

  public ASTEQNode(VmParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(VmParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=8172c9cd707f970190c1785bf017f52a (do not edit this line) */