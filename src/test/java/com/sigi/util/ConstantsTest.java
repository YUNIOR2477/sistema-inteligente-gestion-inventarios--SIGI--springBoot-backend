package com.sigi.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConstantsTest {

    @Test
    void shouldHaveCorrectErrorMessages() {
        assertEquals("No user account was found with the email address: %s. Please verify the email and try again.", Constants.EMAIL_NOT_FOUND);
        assertEquals("Invalid username or password. Please check your credentials and try again.", Constants.INVALID_CREDENTIALS);
        assertEquals("An unexpected internal error occurred. Please try again later.", Constants.INTERNAL_SERVER_ERROR);
        assertEquals("A validation error has occurred. Please review the provided information.", Constants.VALIDATION_ERROR);
        assertEquals("You do not have the necessary permissions to access this resource.", Constants.ACCESS_DENIED);
        assertEquals("The requested resource could not be found.", Constants.RESOURCE_NOT_FOUND);
    }

    @Test
    void shouldHaveCorrectSuccessMessages() {
        assertEquals("Login completed successfully.", Constants.SUCCESSFUL_LOGIN);
        assertEquals("%s has been created successfully.", Constants.CREATED_SUCCESSFULLY);
        assertEquals("%s has been updated successfully.", Constants.UPDATED_SUCCESSFULLY);
        assertEquals("%s has been deleted successfully.", Constants.DELETED_SUCCESSFULLY);
        assertEquals("%s has been retrieved successfully.", Constants.SEARCH_SUCCESSFULLY);
        assertEquals("%s has been activated successfully.", Constants.ENTITY_ACTIVATED_SUCCESSFULLY);
    }

    @Test
    void shouldHaveCorrectEntityMessages() {
        assertEquals("%s was not found in the system.", Constants.ENTITY_NOT_FOUND);
        assertEquals("%s already exists in the system.", Constants.ENTITY_ALREADY_EXISTS);
        assertEquals("%s exists but is inactive, please activate it before proceeding.", Constants.ENTITY_INACTIVE);
        assertEquals("%s is already active.", Constants.ENTITY_ALREADY_ACTIVE);
        assertEquals("%s already deleted previously", Constants.ENTITY_PREVIOUSLY_DELETED);
    }

    @Test
    void shouldHaveCorrectCacheNames() {
        assertEquals("clientById", Constants.CLIENT_BY_ID);
        assertEquals("allActiveClients", Constants.ALL_ACTIVE_CLIENTS);
        assertEquals("productsById", Constants.PRODUCT_BY_ID);
        assertEquals("warehouseById", Constants.WAREHOUSE_BY_ID);
        assertEquals("ordersByUser", Constants.ORDERS_BY_USER);
        assertEquals("notificationById", Constants.NOTIFICATION_BY_ID);
        assertEquals("chatRoomById", Constants.CHAT_ROOM_BY_ID);
    }

    @Test
    void shouldHaveCorrectGeneralConstants() {
        assertEquals("America/Bogota", Constants.DATE_ZONE);
        assertEquals(10, Constants.SIZE_PAGE_DEFAULT);
        assertEquals("correlationId", Constants.CORRELATION_ID);
    }
}