package com.iortatechnxt.brokerverse.organization.domain;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link Employee}. */
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

  /**
   * Whether an employee number is taken.
   *
   * @param companyId company
   * @param employeeNo number
   * @return true when taken
   */
  boolean existsByCompanyIdAndEmployeeNo(Long companyId, String employeeNo);

  /**
   * Employees matching a text (number, name, cost centre), active first.
   *
   * @param companyId company
   * @param text lower-case fragment, empty for all
   * @param pageable page
   * @return employees
   */
  @Query(
      """
      select e from Employee e
      where e.companyId = :companyId
        and (:text = '' or lower(e.employeeNo) like concat('%', :text, '%')
             or lower(e.fullName) like concat('%', :text, '%')
             or lower(e.costCenter) like concat('%', :text, '%'))
      order by e.active desc, e.fullName
      """)
  Page<Employee> search(
      @Param("companyId") Long companyId, @Param("text") String text, Pageable pageable);

  /**
   * Active employees of a company.
   *
   * @param companyId company
   * @return employees by cost centre and name
   */
  List<Employee> findByCompanyIdAndActiveTrueOrderByCostCenterAscFullNameAsc(Long companyId);
}
