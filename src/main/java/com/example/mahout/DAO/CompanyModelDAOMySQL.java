package com.example.mahout.DAO;

import com.example.mahout.entity.CompanyModel;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class CompanyModelDAOMySQL implements CompanyModelDAO {

    public static final String COMPANY_NAME = "COMPANY_NAME";
    public static final String PROPERTY = "PROPERTY";
    public static final String MODEL = "MODEL";
    public static final String LABELINDEX = "LABELINDEX";
    public static final String DICTIONARY = "DICTIONARY";
    public static final String FREQUENCIES = "FREQUENCIES";
    private static String url = "jdbc:sqlite:database/mahout_api.db";
    private static String username = "root";
    private static String ps = "root";

    private Connection c;

    public CompanyModelDAOMySQL() throws SQLException {

        c = DriverManager.getConnection(url, username, ps);
    }

    @Override
    public CompanyModel findOne(String companyName, String property) throws SQLException, IOException {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = c.prepareStatement("SELECT * FROM file_model_documents WHERE COMPANY_NAME = ? AND PROPERTY=?");
            ps.setString(1, companyName);
            ps.setString(2, property);
            ps.execute();
            rs = ps.getResultSet();

            if (rs.next()) {
                CompanyModel result = new CompanyModel(
                        rs.getString(COMPANY_NAME),
                        rs.getString(PROPERTY),
                        rs.getBytes(MODEL),
                        rs.getBytes(LABELINDEX),
                        rs.getBytes(DICTIONARY),
                        rs.getBytes(FREQUENCIES));
                ps.close();
                rs.close();
                return result;

            } else {
                ps.close();
                rs.close();
                return null;
            }
        } finally {
            ps.close();
            rs.close();
        }
    }

    @Override
    public List<CompanyModel> findByCompany(String company) throws SQLException, IOException {

        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = c.prepareStatement("SELECT * FROM file_model_documents WHERE COMPANY_NAME = ?");
            ps.setString(1, company);
            ps.execute();
            rs = ps.getResultSet();

            List<CompanyModel> fileModels = new ArrayList<>();
            while (rs.next()) {
                fileModels.add(new CompanyModel(
                        rs.getString(COMPANY_NAME),
                        rs.getString(PROPERTY),
                        rs.getBytes(MODEL),
                        rs.getBytes(LABELINDEX),
                        rs.getBytes(DICTIONARY),
                        rs.getBytes(FREQUENCIES)));
            }
            ps.close();
            rs.close();

            return fileModels;
        } finally {
            ps.close();
            rs.close();
        }
    }

    @Override
    public boolean save(CompanyModel fileModel) throws SQLException {

        PreparedStatement ps = null;
        try {
            ps = c.prepareStatement("INSERT INTO file_model_documents VALUES (?, ?, ?, ?, ?, ?)");
            ps.setString(1, fileModel.getCompanyName());
            ps.setString(2, fileModel.getProperty());
            ps.setBytes(3, fileModel.getModel());
            ps.setBytes(4, fileModel.getLabelindex());
            ps.setBytes(5, fileModel.getDictionary());
            ps.setBytes(6, fileModel.getFrequencies());
            int result = ps.executeUpdate();
            ps.close();
            return result != 0;
        } finally {
            ps.close();
        }
    }

    @Override
    public boolean delete(String companyName, String property) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = c.prepareStatement("DELETE FROM file_model_documents WHERE COMPANY_NAME = ? AND PROPERTY=?");
            ps.setString(1, companyName);
            ps.setString(2, property);
            int result = ps.executeUpdate();
            ps.close();
            return result != 0;
        } finally {
            ps.close();
        }
    }

    @Override
    public boolean deleteByCompany(String companyName) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = c.prepareStatement("DELETE FROM file_model_documents WHERE COMPANY_NAME = ?");
            ps.setString(1, companyName);
            int result = ps.executeUpdate();
            ps.close();
            return result != 0;
        } finally {
            ps.close();
        }
    }

    @Override
    public boolean deleteAll() throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = c.prepareStatement("DELETE FROM file_model_documents");
            int result = ps.executeUpdate();
            ps.close();
            return result != 0;
        } finally {
            ps.close();
        }
    }

    @Override
    public boolean deleteAllMulti(String companyName, String property) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = c.prepareStatement("DELETE FROM file_model_documents WHERE COMPANY_NAME = ? AND PROPERTY LIKE ? || '%'");
            ps.setString(1, companyName);
            ps.setString(2, property);
            int result = ps.executeUpdate();
            ps.close();
            return result != 0;
        } finally {
            ps.close();
        }
    }

    @Override
    public List<CompanyModel> findAllMulti(String enterpriseName, String property) throws SQLException, IOException {
        PreparedStatement ps = null;
        try {
            ps = c.prepareStatement("SELECT * FROM file_model_documents WHERE COMPANY_NAME = ? AND PROPERTY LIKE ? || '%'");
            ps.setString(1, enterpriseName);
            ps.setString(2, property);
            ps.execute();
            ResultSet rs = ps.getResultSet();

            List<CompanyModel> fileModels = new ArrayList<>();
            while (rs.next()) {
                fileModels.add(new CompanyModel(
                        rs.getString(COMPANY_NAME),
                        rs.getString(PROPERTY),
                        rs.getBytes(MODEL),
                        rs.getBytes(LABELINDEX),
                        rs.getBytes(DICTIONARY),
                        rs.getBytes(FREQUENCIES)));
            }
            ps.close();
            rs.close();

            return fileModels;
        } finally {
            ps.close();
        }
    }

    @Override
    public List<CompanyModel> findAll() throws SQLException, IOException {

        PreparedStatement ps = null;
        try {
            ps = c.prepareStatement("SELECT * FROM file_model_documents");
            ps.execute();
            ResultSet rs = ps.getResultSet();

            List<CompanyModel> fileModels = new ArrayList<>();
            while (rs.next()) {
                fileModels.add(new CompanyModel(
                        rs.getString(COMPANY_NAME),
                        rs.getString(PROPERTY),
                        rs.getBytes(MODEL),
                        rs.getBytes(LABELINDEX),
                        rs.getBytes(DICTIONARY),
                        rs.getBytes(FREQUENCIES)));
            }
            ps.close();
            rs.close();

            return fileModels;
        } finally {
            ps.close();
        }

    }

    @Override
    public boolean exists(String companyName, String property) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = c.prepareStatement("SELECT * FROM file_model_documents WHERE COMPANY_NAME = ? AND PROPERTY = ?");
            ps.setString(1, companyName);
            ps.setString(2, property);
            ps.execute();
            ResultSet rs = ps.getResultSet();
            boolean result = rs.next();
            ps.close();
            rs.close();
            return result;
        } finally {
            ps.close();
        }
    }

    @Override
    public boolean update(CompanyModel fileModel) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = c.prepareStatement("UPDATE file_model_documents SET MODEL=?, LABELINDEX=?, DICTIONARY=?, FREQUENCIES=? WHERE COMPANY_NAME=? AND PROPERTY = ?");
            ps.setBytes(1, fileModel.getModel());
            ps.setBytes(2, fileModel.getLabelindex());
            ps.setBytes(3, fileModel.getDictionary());
            ps.setBytes(4, fileModel.getFrequencies());
            ps.setString(5, fileModel.getCompanyName());
            ps.setString(6, fileModel.getProperty());
            int updated = ps.executeUpdate();
            ps.close();
            return updated != 0;
        } finally {
            ps.close();
        }
    }

}
