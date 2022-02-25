package authentication

import scala.language.postfixOps
import scala.language.implicitConversions

/**
 * Trait to allow combining role requirements using and, or, not
 */
// Not sure this is really necessary, but it was fun (hence the fun symbolic operators)
sealed trait RoleQuery { self =>
  def isEligible(userRoles: List[String]): Boolean = self match {
    case Role(role) => userRoles.contains(role)
    case And(roles) => roles.forall(q => q.isEligible(userRoles))
    case Or(roles) => roles.exists(q => q.isEligible(userRoles))
    case Not(roles) => roles.forall(q => !q.isEligible(userRoles))
  }

  def and(that: RoleQuery): RoleQuery = And(Seq(self, that))
  def or(that: RoleQuery): RoleQuery = Or(Seq(self, that))
  def not: RoleQuery = Not(Seq(self))

  // alias for and
  def ~&~(that: RoleQuery): RoleQuery = and(that)

  // alias for or
  def ~|~(that: RoleQuery): RoleQuery = or(that)

  // alias for not
  def unary_! : RoleQuery = not
}

case class Role(role: String) extends RoleQuery
case class And(roles: Seq[RoleQuery]) extends RoleQuery
case class Or(roles: Seq[RoleQuery]) extends RoleQuery
case class Not(roles: Seq[RoleQuery]) extends RoleQuery

object RoleQuery {
  // Implicit conversion will turn the string into a Role using and then use the appropriate operators on RoleQuery...so cool
  implicit def stringToRuleQuery(s: String) = Role(s)
}

