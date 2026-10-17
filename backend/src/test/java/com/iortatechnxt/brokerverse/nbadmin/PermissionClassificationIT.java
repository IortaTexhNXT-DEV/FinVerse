package com.iortatechnxt.brokerverse.nbadmin;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.nbadmin.service.AccessMatrix;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessMatrixService;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Every permission of the User Access Matrix has its functional area and action class. */
@IntegrationTest
class PermissionClassificationIT {

  @Autowired private AccessMatrixService matrix;

  @Test
  void everyPermissionHasAnAreaAndAnActionClass() {
    List<String> unclassified =
        matrix.matrix().permissions().stream()
            .filter(p -> p.area() == null || p.actions().isEmpty())
            .map(AccessMatrix.PermissionRow::permission)
            .toList();
    assertThat(unclassified).as("permissions without area or action class").isEmpty();
  }
}
