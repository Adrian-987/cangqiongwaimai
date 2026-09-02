package com.sky.controller.admin;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录:{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }


    //新增员工接口,带自定义异常处理,threadlocal存储操作人信息
    @PostMapping
    public Result insertEmp(@RequestBody EmployeeDTO employeeDTO){
        log.info("新增员工信息为{}",employeeDTO);
        employeeService.insertEmp(employeeDTO);
        return Result.success();
    }

    //分页查询接口,消息转换器转化时间格式
    @GetMapping("/page")
    public Result<PageResult> selectEmpPage(EmployeePageQueryDTO employeePageQueryDTO){
        log.info("分页查询{}",employeePageQueryDTO);
        PageResult pageResult=employeeService.selectEmpPage(employeePageQueryDTO);
        return Result.success(pageResult);
    }

    //修改账户状态
    @PostMapping("/status/{status}")
    public Result updateStatus(@PathVariable Integer status,Long id){
        log.info("修改状态码{},id为{}",status,id);
        employeeService.uodateStatus(status,id);
        return Result.success();
    }

    //员工查询回显
    @GetMapping("/{id}")
    public Result<Employee> selectByid(@PathVariable Long id){
        log.info("查询id为{}的信息",id);
        Employee employee=employeeService.selectByid(id);
        return Result.success(employee);
    }

    //修改员工信息
    @PutMapping
    public Result updateEmp(@RequestBody EmployeeDTO employeeDTO){
        log.info("修改员工信息为{}",employeeDTO);
        employeeService.updateEmp(employeeDTO);
        return Result.success();
    }
    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success();
    }

}
