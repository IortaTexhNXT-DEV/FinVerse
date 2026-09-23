package com.iortatechnxt.finverse.finreport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.coa.domain.AccountClass;
import com.iortatechnxt.finverse.coa.domain.AccountLevel;
import com.iortatechnxt.finverse.finreport.service.AccountHierarchy;
import com.iortatechnxt.finverse.finreport.service.AccountNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccountHierarchyTest {

  static AccountNode node(long id, String code, AccountLevel level, Long parent, boolean postable) {
    return new AccountNode(
        id,
        code,
        "Account " + code,
        level,
        parent,
        AccountClass.ASSET,
        postable,
        false,
        false,
        null);
  }

  /** 1000 GROUP > 1100 MAIN > 1110 SUB > 1111 MICRO; 1400 MAIN postable; 8000 GROUP > 8001 SUB. */
  static AccountHierarchy chart() {
    return new AccountHierarchy(
        List.of(
            node(1, "1000", AccountLevel.GROUP, null, false),
            node(2, "1100", AccountLevel.MAIN, 1L, false),
            node(3, "1110", AccountLevel.SUB, 2L, false),
            node(4, "1111", AccountLevel.MICRO, 3L, true),
            node(5, "1120", AccountLevel.SUB, 2L, true),
            node(6, "1400", AccountLevel.MAIN, 1L, true),
            node(7, "8000", AccountLevel.GROUP, null, false),
            node(8, "8001", AccountLevel.SUB, 7L, true)));
  }

  @Test
  void subAndMicroAccountsRollUpToTheirMainAccount() {
    AccountHierarchy h = chart();
    assertThat(h.mainOf(4L).code()).isEqualTo("1100");
    assertThat(h.mainOf(5L).code()).isEqualTo("1100");
    assertThat(h.mainOf(3L).code()).isEqualTo("1100");
    assertThat(h.isSubAccount(4L)).isTrue();
  }

  @Test
  void postableMainAccountIsItsOwnMain() {
    AccountHierarchy h = chart();
    assertThat(h.mainOf(6L).code()).isEqualTo("1400");
    assertThat(h.isSubAccount(6L)).isFalse();
  }

  @Test
  void accountWithoutMainLevelUsesHighestNonGroupAncestor() {
    assertThat(chart().mainOf(8L).code()).isEqualTo("8001");
  }

  @Test
  void mainAccountsAndPostableFilters() {
    AccountHierarchy h = chart();
    assertThat(h.mainAccounts())
        .extracting(AccountNode::code)
        .containsExactly("1100", "1400", "8001");
    assertThat(h.postableIds(m -> "1100".equals(m.code()), a -> true))
        .containsExactlyInAnyOrder(4L, 5L);
    assertThat(h.postableIds(m -> true, a -> a.code().startsWith("11")))
        .containsExactlyInAnyOrder(4L, 5L);
    assertThat(h.all()).hasSize(8);
    assertThat(h.byCode("1400")).map(AccountNode::id).contains(6L);
    assertThat(h.node(2L).caption()).isEqualTo("1100 - Account 1100");
  }

  @Test
  void cyclicParentsDoNotLoopForever() {
    AccountHierarchy h =
        new AccountHierarchy(
            List.of(
                node(1, "A", AccountLevel.SUB, 2L, true),
                node(2, "B", AccountLevel.SUB, 1L, true)));
    assertThat(h.mainOf(1L)).isNotNull();
  }

  @Test
  void unknownAccountIsRejected() {
    assertThatThrownBy(() -> chart().node(99L)).isInstanceOf(IllegalArgumentException.class);
  }
}
