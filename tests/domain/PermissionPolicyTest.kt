package domain

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import support.path

class PermissionPolicyTest {

    private fun grant(raw: String, vararg actions: Action) = Grant(path(raw), actions.toSet())

    @Test
    fun shouldDenyWhenUserHasNoGrants() {
        assertFalse(PermissionPolicy.isAllowed(emptyList(), path("A"), Action.READ))
    }

    @Test
    fun shouldAllowWhenGrantIsOnTheSameResource() {
        assertTrue(PermissionPolicy.isAllowed(listOf(grant("A.B", Action.READ)), path("A.B"), Action.READ))
    }

    @Test
    fun shouldAllowChildWhenGrantIsOnParent() {
        assertTrue(PermissionPolicy.isAllowed(listOf(grant("A", Action.READ)), path("A.B.C"), Action.READ))
    }

    @Test
    fun shouldDenyParentWhenGrantIsOnlyOnChild() {
        assertFalse(PermissionPolicy.isAllowed(listOf(grant("A.B", Action.READ)), path("A"), Action.READ))
    }

    @Test
    fun shouldDenyWhenActionIsNotInGrant() {
        assertFalse(PermissionPolicy.isAllowed(listOf(grant("A", Action.READ)), path("A"), Action.WRITE))
    }

    @Test
    fun shouldAllowEachActionListedInGrant() {
        val grants = listOf(grant("A", Action.READ, Action.WRITE, Action.EXECUTE))
        assertTrue(Action.values().all { PermissionPolicy.isAllowed(grants, path("A.B"), it) })
    }

    @Test
    fun shouldDenySiblingBranchWhenGrantIsOnAnotherBranch() {
        assertFalse(PermissionPolicy.isAllowed(listOf(grant("A.B", Action.READ)), path("A.D"), Action.READ))
    }

    @Test
    fun shouldDenySiblingWithSamePrefixInName() {
        assertFalse(PermissionPolicy.isAllowed(listOf(grant("A.B", Action.READ)), path("A.BX"), Action.READ))
    }

    @Test
    fun shouldAllowWhenAnyOfSeveralGrantsMatches() {
        val grants = listOf(grant("A", Action.READ), grant("Docs", Action.EXECUTE))
        assertTrue(PermissionPolicy.isAllowed(grants, path("Docs.Reports"), Action.EXECUTE))
    }

    @Test
    fun shouldNotCombineActionFromOneGrantWithPathFromAnother() {
        val grants = listOf(grant("A", Action.READ), grant("Docs", Action.EXECUTE))
        assertFalse(PermissionPolicy.isAllowed(grants, path("A"), Action.EXECUTE))
    }
}
