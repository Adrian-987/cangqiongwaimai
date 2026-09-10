package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.exception.AddressBookBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Slf4j
public class AddressBookServiceImpl implements AddressBookService {
    @Autowired
    private AddressBookMapper addressBookMapper;

    /**
     * 条件查询
     *
     * @param addressBook
     * @return
     */
    public List<AddressBook> list(AddressBook addressBook) {
        return addressBookMapper.list(addressBook);
    }

    /**
     * 新增地址
     *
     * @param addressBook
     */
    public void save(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setIsDefault(0);
        addressBookMapper.insert(addressBook);
    }

    /**
     * 根据id查询（仅能查询当前登录用户自己的地址）
     *
     * @param id
     * @return
     */
    public AddressBook getById(Long id) {
        return getOwnedAddressBook(id);
    }

    /**
     * 根据id修改地址（仅能修改当前登录用户自己的地址）
     *
     * @param addressBook
     */
    public void update(AddressBook addressBook) {
        //先校验归属，防止通过伪造id修改他人地址
        getOwnedAddressBook(addressBook.getId());
        addressBookMapper.update(addressBook);
    }

    /**
     * 设置默认地址（仅能操作当前登录用户自己的地址）
     *
     * @param addressBook
     */
    @Transactional
    public void setDefault(AddressBook addressBook) {
        //先校验归属，防止把他人地址设为当前用户的默认地址
        getOwnedAddressBook(addressBook.getId());

        //1、将当前用户的所有地址修改为非默认地址 update address_book set is_default = ? where user_id = ?
        addressBook.setIsDefault(0);
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBookMapper.updateIsDefaultByUserId(addressBook);

        //2、将当前地址改为默认地址 update address_book set is_default = ? where id = ?
        addressBook.setIsDefault(1);
        addressBookMapper.update(addressBook);
    }

    /**
     * 根据id删除地址（仅能删除当前登录用户自己的地址）
     *
     * @param id
     */
    public void deleteById(Long id) {
        //先校验归属，防止通过伪造id删除他人地址
        getOwnedAddressBook(id);
        addressBookMapper.deleteById(id);
    }

    /**
     * 查询地址并校验其归属当前登录用户。
     * 地址不存在或不属于当前用户时统一抛出业务异常：既避免越权访问（IDOR），
     * 也避免通过不同的错误信息暴露他人数据是否存在。
     *
     * @param id 地址id
     * @return 归属当前用户的地址
     */
    private AddressBook getOwnedAddressBook(Long id) {
        Long userId = BaseContext.getCurrentId();
        AddressBook addressBook = addressBookMapper.getById(id);
        if (addressBook == null || !addressBook.getUserId().equals(userId)) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_NOT_FOUND);
        }
        return addressBook;
    }

}
