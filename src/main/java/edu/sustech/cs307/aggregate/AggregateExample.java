package edu.sustech.cs307.aggregate;

import edu.sustech.cs307.meta.TabCol;

/**
 * Example demonstrating aggregate function usage
 * 
 * SQL Examples that this framework now supports:
 * 
 * 1. SELECT MIN(gpa) FROM student GROUP BY department;
 * 2. SELECT department, COUNT(*), AVG(gpa) FROM student GROUP BY department;
 * 3. SELECT MAX(salary), MIN(salary) FROM employee;
 * 4. SELECT SUM(credits) FROM course WHERE department = 'CS';
 */
public class AggregateExample {
    
    public static void demonstrateUsage() {
        // Example 1: MIN aggregate
        TabCol gpaColumn = new TabCol("student", "gpa");
        AggregateExpression minGpa = new AggregateExpression(
            AggregateFunction.MIN, 
            gpaColumn, 
            "min_gpa"
        );
        
        // Example 2: COUNT aggregate
        TabCol countColumn = new TabCol("student", "*");
        AggregateExpression countStudents = new AggregateExpression(
            AggregateFunction.COUNT, 
            countColumn, 
            "student_count"
        );
        
        // Example 3: AVG aggregate
        AggregateExpression avgGpa = new AggregateExpression(
            AggregateFunction.AVG, 
            gpaColumn, 
            "average_gpa"
        );
        
        // Example 4: SUM aggregate with DISTINCT
        TabCol creditsColumn = new TabCol("course", "credits");
        AggregateExpression sumCredits = new AggregateExpression(
            AggregateFunction.SUM, 
            creditsColumn, 
            "total_credits", 
            true // DISTINCT
        );
        
        System.out.println("Aggregate Examples:");
        System.out.println("1. " + minGpa);
        System.out.println("2. " + countStudents);
        System.out.println("3. " + avgGpa);
        System.out.println("4. " + sumCredits);
    }
    
    /**
     * Data flow example for: SELECT department, MIN(gpa), COUNT(*) FROM student GROUP BY department
     * 
     * Input data:
     * [(dept=CS, gpa=3.5, name=Alice), (dept=CS, gpa=3.8, name=Bob), 
     *  (dept=Math, gpa=3.2, name=Charlie), (dept=Math, gpa=3.9, name=David)]
     * 
     * After GROUP BY department:
     * Group 1 (CS): [(dept=CS, gpa=3.5, name=Alice), (dept=CS, gpa=3.8, name=Bob)]
     * Group 2 (Math): [(dept=Math, gpa=3.2, name=Charlie), (dept=Math, gpa=3.9, name=David)]
     * 
     * After aggregation:
     * Result 1: (department=CS, min_gpa=3.5, student_count=2)
     * Result 2: (department=Math, min_gpa=3.2, student_count=2)
     */
}