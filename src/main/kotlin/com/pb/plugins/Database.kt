package com.pb.plugins

import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.pb.Constants.AdminCollectionName
import com.pb.Constants.CoordinatorCollectionName
import com.pb.Constants.FreelancerCollectionName
import com.pb.models.employee.SavedEmployee
import com.pb.models.employee.TransactionEmployee
import com.pb.models.inquiry.SavedInquiry
import kotlinx.coroutines.runBlocking


object MongoDb {
    private val mongoDbInstance = MongoClient.create("mongodb+srv://yusufjamal1372:SsiifBCm-8tRD-N@cluster0.nfanunr.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0")
    val mongoDb = mongoDbInstance.getDatabase("PB_db")

    init {
        val adminCollection = mongoDb.getCollection<SavedEmployee>(AdminCollectionName)
        val coordinatorCollection = mongoDb.getCollection<SavedEmployee>(CoordinatorCollectionName)
        val freelancerCollection = mongoDb.getCollection<SavedEmployee>(FreelancerCollectionName)
        val inquiryCollection = mongoDb.getCollection<SavedInquiry>("Inquiry")

        runBlocking {
            adminCollection.createIndex(Indexes.ascending(TransactionEmployee::employeeId.name), IndexOptions().unique(true))
            coordinatorCollection.createIndex(Indexes.ascending(TransactionEmployee::employeeId.name), IndexOptions().unique(true))
            freelancerCollection.createIndex(Indexes.ascending(TransactionEmployee::employeeId.name), IndexOptions().unique(true))
            inquiryCollection.createIndex(Indexes.ascending(SavedInquiry::inquiryId.name), IndexOptions().unique(true))
        }
    }
}