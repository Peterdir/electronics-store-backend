package com.ecommerce.backend.common.utils;

import com.github.f4b6a3.tsid.TsidCreator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;

public class TsidIdentifierGenerator implements IdentifierGenerator {

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        // Sinh mã TSID và trả về dưới dạng số nguyên Long (chiếm 8 bytes trong DB)
        return TsidCreator.getTsid().toLong();
    }
}
