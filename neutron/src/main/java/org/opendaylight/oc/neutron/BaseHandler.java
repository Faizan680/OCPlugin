/*
 * Copyright (c) 2013 Juniper Networks, Inc. All rights reserved.
 *
 */
package org.opendaylight.oc.neutron;

import java.net.HttpURLConnection;
import java.util.UUID;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base utility functions used by neutron handlers.
 */
public class BaseHandler {

    /**
     * Logger instance.
     */
    static final Logger LOGGER = LoggerFactory.getLogger(BaseHandler.class);

    /**
     * Neutron UUID identifier length.
     */
    private static final int UUID_LEN = 36;

    /**
     * Tenant id length when keystone identifier is used in neutron.
     */
    private static final int KEYSTONE_ID_LEN = 32;

    /**
     * UUID version number position.
     */
    private static final int UUID_VERSION_POS = 12;

    /**
     * UUID time-low field byte length in hex.
     */
    private static final int UUID_TIME_LOW = 8;

    /**
     * UUID time-mid field byte length in hex.
     */
    private static final int UUID_TIME_MID = 4;

    /**
     * UUID time-high and version field byte length in hex.
     */
    private static final int UUID_TIME_HIGH_VERSION = 4;

    /**
     * UUID clock sequence field byte length in hex.
     */
    private static final int UUID_CLOCK_SEQ = 4;

    /**
     * UUID node field byte length in hex.
     */
    private static final int UUID_NODE = 12;

    /**
     * UUID time field byte length in hex.
     */
    private static final int UUID_TIME_LEN = (UUID_TIME_LOW +
            UUID_TIME_MID + UUID_TIME_HIGH_VERSION);

    /**
     * Convert failure status returned by the  manager into
     * neutron API service errors.
     *
     * @param status  manager status
     * @return  An error to be returned to neutron API service.
     */
    protected static final int getException(Status status) {
        int result = HttpURLConnection.HTTP_INTERNAL_ERROR;

        assert !status.isSuccess();

        StatusCode code = status.getCode();
        LOGGER.debug(" Execption code - {}, description - {}",
                code, status.getDescription());

        if (code == StatusCode.BADREQUEST) {
            result = HttpURLConnection.HTTP_BAD_REQUEST;
        } else if (code == StatusCode.CONFLICT) {
            result = HttpURLConnection.HTTP_CONFLICT;
        } else if (code == StatusCode.NOTACCEPTABLE) {
            result = HttpURLConnection.HTTP_NOT_ACCEPTABLE;
        } else if (code == StatusCode.NOTFOUND) {
            result = HttpURLConnection.HTTP_NOT_FOUND;
        } else {
            result = HttpURLConnection.HTTP_INTERNAL_ERROR;
        }

        return result;
    }

    /**
     * Verify the validity of neutron object identifiers.
     *
     * @param id neutron object id.
     * @return {@code true} neutron identifier is valid.
     *         {@code false} neutron identifier is invalid.
     */
    protected static final boolean isValidNeutronID(String id) {
        if (id == null) {
            return false;
        }
        boolean isValid = false;
        LOGGER.trace("id - {}, length - {} ", id, id.length());
        /**
         * check the string length
         * if length is 36 its a uuid do uuid validation
         * if length is 32 it can be tenant id form keystone
         * if its less than 32  can be valid  ID
         */
        if (id.length() == UUID_LEN) {
            try {
                UUID fromUUID = UUID.fromString(id);
                String toUUID = fromUUID.toString();
                isValid = toUUID.equalsIgnoreCase(id);
            } catch(IllegalArgumentException e) {
                LOGGER.error(" IllegalArgumentExecption for id - {} ", id);
                isValid = false;
            }
        } else if ((id.length() > 0) && (id.length() <= KEYSTONE_ID_LEN)) {
            isValid = true;
        } else {
            isValid = false;
        }
        return isValid;
    }

    /**
     * Convert UUID to  key syntax.
     *
     * @param id neutron object UUID.
     * @return key in compliance to  object key.
     */
    private static String convertUUIDToKey(String id) {

        String key = null;
        if (id == null) {
            return key;
        }
        LOGGER.trace("id - {}, length - {} ", id, id.length());
        /**
         *  ID must be less than 32 bytes,
         * Shorten UUID string length from 36 to 31 as follows:
         * delete UUID Version and hyphen (see RFC4122) field in the UUID
         */
        try {
            StringBuilder tKey = new StringBuilder();
            // remove all the '-'
            for (String retkey: id.split("-")) {
                tKey.append(retkey);
            }
            // remove the version byte
            tKey.deleteCharAt(UUID_VERSION_POS);
            key = tKey.toString();
        } catch(IllegalArgumentException ile) {
            LOGGER.error(" Invalid UUID - {} ", id);
            key = null;
        }
        return key;
    }

    /**
     * Convert string id to  key syntax.
     *
     * @param id neutron object id.
     * @return key in compliance to  object key.
     */
    private static String convertKeystoneIDToKey(String id) {
        String key = null;
        if (id == null) {
            return key;
        }

        /**
         * tenant ID if given from openstack keystone does not follow the
         * generic UUID syntax, convert the ID to UUID format for validation
         * and reconvert it to  key
         */

        LOGGER.trace(" id - {}, length - {} ", id, id.length());
        try {
            StringBuilder tKey = new StringBuilder();
            String tmpStr = id.substring(0, UUID_TIME_LOW);
            tKey.append(tmpStr);
            tKey.append("-");
            tmpStr = id.substring(UUID_TIME_LOW,
                    (UUID_TIME_LOW + UUID_TIME_MID));
            tKey.append(tmpStr);
            tKey.append("-");
            tmpStr = id.substring((UUID_TIME_LOW + UUID_TIME_MID),
                    UUID_TIME_LEN);
            tKey.append(tmpStr);
            tKey.append("-");
            tmpStr = id.substring(UUID_TIME_LEN,
                    (UUID_TIME_LEN + UUID_CLOCK_SEQ));
            tKey.append(tmpStr);
            tKey.append("-");
            tmpStr = id.substring((UUID_TIME_LEN + UUID_CLOCK_SEQ),
                    (UUID_TIME_LEN + UUID_CLOCK_SEQ + UUID_NODE));
            tKey.append(tmpStr);

            tmpStr = tKey.toString();
            UUID fromUUID = UUID.fromString(tmpStr);
            String toUUID = fromUUID.toString();
            if (toUUID.equalsIgnoreCase(tmpStr)) {
                key = convertUUIDToKey(tmpStr);
            }
        } catch(IndexOutOfBoundsException ibe) {
            LOGGER.error(" Execption! Invalid UUID - {} ", id);
            key = null;
        } catch (IllegalArgumentException iae) {
            LOGGER.error(" Execption! Invalid object ID - {} ", id);
            key = null;
        }
        return key;
    }

    /**
     * Convert neutron object id to  key syntax.
     *
     * @param neutronID neutron object id.
     * @return key in compliance to  object key.
     */
    protected static final String convertNeutronIDToKey(String neutronID) {
        String key = null;
        if (neutronID == null) {
            return key;
        }

        LOGGER.trace(" neutronID - {}, length - {} ",
                neutronID, neutronID.length());
        if (!isValidNeutronID(neutronID)) {
            return key;
        }

        if (neutronID.length() == UUID_LEN) {
            key = convertUUIDToKey(neutronID);
        } else if (neutronID.length() == KEYSTONE_ID_LEN) {
            key = convertKeystoneIDToKey(neutronID);
        } else {
            key = neutronID;
        }
        return key;
    }

}