package com.example.mahout.dao;

import com.example.mahout.entity.CompanyModel;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;


public interface CompanyModelDAO {

    CompanyModel findOne(String name, String property) throws SQLException, IOException;

    List<CompanyModel> findByCompany(String company) throws SQLException, IOException;

    boolean save(CompanyModel fileModel) throws SQLException;

    boolean delete(String name, String property) throws SQLException;

    boolean deleteByCompany(String name) throws SQLException;

    boolean deleteAll() throws SQLException;

    boolean deleteAllMulti(String company, String property) throws SQLException;

    List<CompanyModel> findAllMulti(String enterpriseName, String property) throws SQLException, IOException;

    List<CompanyModel> findAll() throws SQLException, IOException;

    boolean exists(String name, String property) throws SQLException;

    boolean update(CompanyModel fileModel) throws SQLException;

}
